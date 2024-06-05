/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package top.linl.hook;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.util.HostInfo;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.Toasts;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import top.linl.util.reflect.ClassUtils;
import top.linl.util.reflect.FieldUtils;
import top.linl.util.reflect.MethodTool;
import xyz.nextalone.hook.CleanRecentChat;

/**
 * @author suzhelan
 * @CreateDate 2023.12.18
 */
public class FixCleanRecentChat {

    private static final ConcurrentHashMap<Object, Integer> viewHolderList = new ConcurrentHashMap<>();
    private static int deleteTextViewId;
    private final CleanRecentChat cleanRecentChat;

    private Activity activity;

    public FixCleanRecentChat(CleanRecentChat cleanRecentChat) {
        this.cleanRecentChat = cleanRecentChat;
    }


    private void hookGetDeleteViewId() {
        Class<?> superClass = ClassUtils.getClass("com.tencent.qqnt.chats.biz.guild.GuildDiscoveryItemBuilder").getSuperclass();
        Class<?> findClass = null;
        for (Field field : superClass.getDeclaredFields()) {
            field.setAccessible(true);
            Class<?> type = field.getType();
            if (type.getName().startsWith("com.tencent.qqnt.chats.core.adapter.")) {
                findClass = type;
                break;
            }
        }
        Method method = MethodTool.find(findClass).params(
                        android.view.ViewGroup.class,
                        java.util.List.class
                ).returnType(List.class)
                .get();
        HookUtils.hookAfterIfEnabled(cleanRecentChat, method, param -> {
            if (deleteTextViewId != 0) {
                return;
            }
            List<View> viewList = (List<View>) param.getResult();
            for (View view : viewList) {
                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    if (textView.getText().toString().equals("删除")) {
                        deleteTextViewId = textView.getId();
                        break;
                    }
                }
            }
        });

    }

    public void loadHook() throws Exception {
        hookGetDeleteViewId();
        hookOnHolder();

        //不hook onCreate方法了 那样需要重启才能生效 hook onResume可在界面重新渲染到屏幕时会调用生效
        Method onCreateMethod = MethodTool.find("com.tencent.mobileqq.activity.home.Conversation")
                .name(HostInfo.requireMinQQVersion(QQVersion.QQ_9_0_55) ? "notifyResume" : "onResume")
                .params(boolean.class)
                .get();
        HookUtils.hookAfterIfEnabled(cleanRecentChat, onCreateMethod, param -> {
            for (Field field : param.thisObject.getClass().getDeclaredFields()) {
                if (field.getType() == ImageView.class) {
                    field.setAccessible(true);
                    ImageView img = (ImageView) field.get(param.thisObject);
                    if (img != null && "快捷入口".equals(img.getContentDescription().toString())) {
                        img.setOnLongClickListener(v -> {
                            cleanRecentChat.showDialog(img.getContext());
                            return true;
                        });
                    }
                }
            }
        });

    }

    private void hookOnHolder() {
        //find
        Class<?> recentContactItemHolderClass = ClassUtils.getClass("com.tencent.qqnt.chats.core.adapter.holder.RecentContactItemHolder");
        Method onHolderBindTimeingCallSetOnClickMethod = null;
        for (Method method : recentContactItemHolderClass.getDeclaredMethods()) {
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length == 3) {
                if (paramTypes[0].getName().startsWith("com.tencent.qqnt.chats.core.adapter.builder.")
                        && paramTypes[1].getName().startsWith("com.tencent.qqnt.chats.core.adapter.")
                        && paramTypes[2] == int.class) {
                    method.setAccessible(true);
                    onHolderBindTimeingCallSetOnClickMethod = method;
                    break;
                }
            }
        }
        HookUtils.hookBeforeIfEnabled(cleanRecentChat, onHolderBindTimeingCallSetOnClickMethod, param -> {
            int adapterIndex = (int) param.args[2];
            Object item = param.args[1];
            //Holder在前 索引在后 因为Holder在复用池中所以引用地址不会变 但是索引在Adapter中是随时变化的
            viewHolderList.put(param.thisObject, adapterIndex);
        });

        Method onCreate = MethodTool.find("com.tencent.qqnt.chats.core.adapter.ChatsListAdapter")
                .name("onCreateViewHolder")
                .params(android.view.ViewGroup.class, int.class)
                .get();
        HookUtils.hookAfterIfEnabled(cleanRecentChat, onCreate, param -> {
            viewHolderList.put(param.getResult(), (int) param.args[1]);
        });
    }

    public static class DeleteAllItemTask implements Runnable {

        private static final AtomicReference<Method> deleteMethod = new AtomicReference<>();
        private static Class<?> utilType;
        private static Field itemField;

        public boolean isDeleteTopMsg = false;
        public String deleteMode;

        public DeleteAllItemTask(String deleteMode) {
            this.deleteMode = deleteMode;
        }

        private Object findItemField(Object viewHolder) throws IllegalAccessException {
            if (itemField != null) {
                return itemField.get(viewHolder);
            }
            for (Field field : viewHolder.getClass().getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object fieldObj = field.get(viewHolder);
                    if (fieldObj == null) {
                        continue;
                    }
                    String toStr = fieldObj.toString();
                    if (toStr.contains("RecentContactChatItem")) {
                        field.setAccessible(true);
                        itemField = field;
                        break;
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            return itemField.get(viewHolder);
        }

        private Class<?> findUtilClassType(Object viewHolder) {
            if (utilType != null) {
                return utilType;
            }
            for (Field field : viewHolder.getClass().getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object fieldObj = field.get(viewHolder);
                    if (fieldObj == null) {
                        continue;
                    }
                    if (fieldObj.getClass().getName().startsWith("com.tencent.qqnt.chats.core.ui.ChatsListVB$")) {
                        utilType = field.getType();
                        break;
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            if (utilType == null) {
                throw new RuntimeException("not find Class , viewHolder ClassName is " + viewHolder.getClass().getName());
            }
            return utilType;
        }

        private Method getDeleteMethod(Object viewHolder) {
            if (deleteMethod.get() != null) {
                return deleteMethod.get();
            }
            Class<?> findClass = findUtilClassType(viewHolder);
            if (findClass == null) {
                throw new RuntimeException("findClass is null");
            }
            Method finalDeleteMethod = MethodTool.find(findClass).params(int.class,//index ?
                            Object.class,//item
                            ClassUtils.getClass("com.tencent.qqnt.chats.core.adapter.holder.RecentContactItemBinding"),//view binder
                            int.class//click view id
                    ).returnType(void.class)
                    .get();
            deleteMethod.set(finalDeleteMethod);
            return deleteMethod.get();
        }

        @Override
        public void run() {
            final AtomicBoolean isStop = new AtomicBoolean(false);
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    isStop.set(true);
                }
            };
            //在2秒内尽量删除
            Timer timer = new Timer();
            timer.schedule(task, 2000);

            Toasts.show("开始清理");
            int deleteQuantity = 0;
            while (!isStop.get()) {
                int size = viewHolderList.size();
                if (size == 0) {
                    try {
                        //停一下等待ItemHolder重新bind到屏幕上 然后继续删除
                        /*
                         * 假设一次能清理屏幕中的 8 个item
                         * 2000 / 100 * 8 = 400 (个item)
                         * 清掉所有聊天项应该戳戳有余 有问题调高延迟和时长应该可以解决
                         */
                        TimeUnit.MILLISECONDS.sleep(100);
                        continue;
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                Iterator<Map.Entry<Object, Integer>> iterator = viewHolderList.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Object, Integer> viewHolderEntry = iterator.next();
                    try {
                        Object recentContactItemHolder = viewHolderEntry.getKey();
                        if (recentContactItemHolder == null) {
                            continue;
                        }
                        //delete util
                        Object util = FieldUtils.getFirstField(recentContactItemHolder, findUtilClassType(recentContactItemHolder));//util run time obj
                        int adapterIndex = viewHolderEntry.getValue();//call param 1
                        /*
                         * { uid=0000,
                         * title=name,
                         * contactType=2,
                         * unreadCount=UnreadInfo(type=2, count=437).count ,
                         * showTime=晚上8:03,
                         * summary=八 ,
                         * isTop=true,
                         * isDraft=false}
                         * */
                        Object itemInfo = findItemField(recentContactItemHolder);//call param 2
                        String itemToString = String.valueOf(itemInfo);
                        //面向字符串编程
                        if (deleteMode.equals("清理群消息")) {
                            if (!itemToString.contains("contactType=2")) {
                                continue;
                            }
                        } else if (deleteMode.equals("清理其他消息")) {
                            if (itemToString.contains("contactType=2") || itemToString.contains("contactType=1")) {
                                continue;
                            }
                        }
                        if (!isDeleteTopMsg) {
                            if (itemToString.contains("isTop=true")) {
                                continue;
                            }
                        }
                        Object itemBinder = FieldUtils.getFirstField(recentContactItemHolder,
                                ClassUtils.getClass("com.tencent.qqnt.chats.core.adapter.holder.RecentContactItemBinding"));//call param 3
                        int viewId = deleteTextViewId;//call param 4
                        getDeleteMethod(recentContactItemHolder).invoke(util, adapterIndex, itemInfo, itemBinder, viewId);
                        deleteQuantity++;
                    } catch (Exception e) {

                    }
                    iterator.remove();
                }
            }
            System.gc();//调用gc 防止viewHolder还没被回收
            Toasts.show("已清理结束 数量" + deleteQuantity + "个");
        }
    }

}
