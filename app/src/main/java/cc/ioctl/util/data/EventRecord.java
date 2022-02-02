/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
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
package cc.ioctl.util.data;

import java.io.Serializable;


public class EventRecord implements Serializable, Cloneable, Comparable {

    public static final long TIME_UNKNOWN = -1L;
    public static final int EVENT_ERROR = 0;
    public static final int EVENT_RESERVED = 1;
    public static final int EVENT_FRIEND_ADD = 2;
    public static final int EVENT_FRIEND_DELETE = 3;
    public static final int EVENT_FRIEND_SIDE = 4;
    public static final int EVENT_NICKNAME_CHANGE = 5;
    public static final int EVENT_REMARK_CHANGE = 6;
    public static final int EVENT_LOCAL_REMARK_CHANGE = 7;
    public static final int EVENT_ACCOUNT_DESTRUCTION = 8;
    private static final long serialVersionUID = 1L;
    public long timeRangeBegin;
    public long timeRangeEnd;
    public int event;
    public long operand;//uin
    public long executor;
    public String before;
    public String after;
    public String extra;
    public int _friendStatus;
    public String _remark;
    public String _nick;


    public String getShowStr() {
        if (_remark != null && _remark.length() > 0) {
            return _remark;
        } else if (_nick != null && _nick.length() > 0) {
            return _nick;
        } else {
            return "" + operand;
        }
    }

    public String getShowStrWithUin() {
        if (_remark != null && _remark.length() > 0) {
            return _remark + "(" + operand + ")";
        } else if (_nick != null && _nick.length() > 0) {
            return _nick + "(" + operand + ")";
        } else {
            return "" + operand;
        }
    }

    @Override
    public int compareTo(Object obj) {
        EventRecord ev = (EventRecord) obj;
        return (int) (ev.timeRangeEnd - this.timeRangeEnd);
    }
}
