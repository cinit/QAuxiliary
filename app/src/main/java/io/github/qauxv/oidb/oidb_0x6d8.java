package io.github.qauxv.oidb;

import com.tencent.mobileqq.pb.MessageMicro;
import com.tencent.mobileqq.pb.PBField;
import com.tencent.mobileqq.pb.PBInt32Field;
import com.tencent.mobileqq.pb.PBStringField;
import com.tencent.mobileqq.pb.PBUInt32Field;
import com.tencent.mobileqq.pb.PBUInt64Field;

// copy from tencent.im.oidb.cmd0x6d8.oidb_0x6d8
@SuppressWarnings({"unused", "SameParameterValue"})
public final class oidb_0x6d8 {

    public static final class GetFileCountReqBody extends MessageMicro<GetFileCountReqBody> { }

    public static final class GetFileCountRspBody extends MessageMicro<GetFileCountRspBody> { }

    public static final class GetFileInfoReqBody extends MessageMicro<GetFileInfoReqBody> {
        static final MessageMicro.FieldMap __fieldMap__ = MessageMicro.initFieldMap(new int[]{8, 16, 24, 34, 40}, new String[]{"uint64_group_code", "uint32_app_id", "uint32_bus_id", "str_file_id", "uint32_field_flag"}, new Object[]{0L, 0, 0, "", 16777215}, GetFileInfoReqBody.class);
        public final PBUInt64Field uint64_group_code = PBField.initUInt64(0);
        public final PBUInt32Field uint32_app_id = PBField.initUInt32(0);
        public final PBUInt32Field uint32_bus_id = PBField.initUInt32(0);
        public final PBStringField str_file_id = PBField.initString("");
        public final PBUInt32Field uint32_field_flag = PBField.initUInt32(16777215);
    }

    public static final class GetFileInfoRspBody extends MessageMicro<GetFileInfoRspBody> {
        static final MessageMicro.FieldMap __fieldMap__ = MessageMicro.initFieldMap(new int[]{8, 18, 26, 34}, new String[]{"int32_ret_code", "str_ret_msg", "str_client_wording", "file_info"}, new Object[]{0, "", "", null}, GetFileInfoRspBody.class);
        public final PBInt32Field int32_ret_code = PBField.initInt32(0);
        public final PBStringField str_ret_msg = PBField.initString("");
        public final PBStringField str_client_wording = PBField.initString("");
        public group_file_common.FileInfo file_info = new group_file_common.FileInfo();
    }

    public static final class GetFileListReqBody extends MessageMicro<GetFileListReqBody> { }

    public static final class GetFileListRspBody extends MessageMicro<GetFileListRspBody> { }

    public static final class GetFilePreviewReqBody extends MessageMicro<GetFilePreviewReqBody> { }

    public static final class GetFilePreviewRspBody extends MessageMicro<GetFilePreviewRspBody> { }
    public static final class GetSpaceReqBody extends MessageMicro<GetSpaceReqBody> { }

    public static final class GetSpaceRspBody extends MessageMicro<GetSpaceRspBody> { }

    public static final class ReqBody extends MessageMicro<ReqBody> {
        static final MessageMicro.FieldMap __fieldMap__ = MessageMicro.initFieldMap(new int[]{10, 18, 26, 34, 42}, new String[]{"file_info_req", "file_list_info_req", "group_file_cnt_req", "group_space_req", "file_preview_req"}, new Object[]{null, null, null, null, null}, ReqBody.class);
        public GetFileInfoReqBody file_info_req = new GetFileInfoReqBody();
        public GetFileListReqBody file_list_info_req = new GetFileListReqBody();
        public GetFileCountReqBody group_file_cnt_req = new GetFileCountReqBody();
        public GetSpaceReqBody group_space_req = new GetSpaceReqBody();
        public GetFilePreviewReqBody file_preview_req = new GetFilePreviewReqBody();
    }

    public static final class RspBody extends MessageMicro<RspBody> {
        static final MessageMicro.FieldMap __fieldMap__ = MessageMicro.initFieldMap(new int[]{10, 18, 26, 34, 42}, new String[]{"file_info_rsp", "file_list_info_rsp", "group_file_cnt_rsp", "group_space_rsp", "file_preview_rsp"}, new Object[]{null, null, null, null, null}, RspBody.class);
        public GetFileInfoRspBody file_info_rsp = new GetFileInfoRspBody();
        public GetFileListRspBody file_list_info_rsp = new GetFileListRspBody();
        public GetFileCountRspBody group_file_cnt_rsp = new GetFileCountRspBody();
        public GetSpaceRspBody group_space_rsp = new GetSpaceRspBody();
        public GetFilePreviewRspBody file_preview_rsp = new GetFilePreviewRspBody();
    }

    private oidb_0x6d8() {
    }
}
