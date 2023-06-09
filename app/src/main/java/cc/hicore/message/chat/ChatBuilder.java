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

package cc.hicore.message.chat;

public class ChatBuilder {
    private final CommonChat chat;
    public static ChatBuilder group(){
        return new ChatBuilder(0);
    }
    public static ChatBuilder user(){
        return new ChatBuilder(1);
    }
    public static ChatBuilder pri(){
        return new ChatBuilder(2);
    }
    private ChatBuilder(){
        chat = new CommonChat();
    }
    private ChatBuilder(int type){
        this();
        chat.type = type;
    }
    public ChatBuilder uid(String uid){
        chat.uid = uid;
        return this;
    }
    public ChatBuilder groupUin(String groupUin){
        chat.groupUin = groupUin;
        return this;
    }
    public ChatBuilder userUin(String userUin){
        chat.userUin = userUin;
        return this;
    }
    public CommonChat build(){
        return chat;
    }
}
