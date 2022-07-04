package io.github.qauxv.oidb;

import com.tencent.mobileqq.pb.ByteStringMicro;
import com.tencent.mobileqq.pb.MessageMicro;
import com.tencent.mobileqq.pb.PBBytesField;
import com.tencent.mobileqq.pb.PBField;
import com.tencent.mobileqq.pb.PBInt32Field;
import com.tencent.mobileqq.pb.PBStringField;
import com.tencent.mobileqq.pb.PBUInt32Field;
import com.tencent.mobileqq.pb.PBUInt64Field;

// copy from tencent.im.oidb.cmd0x6d8.oidb_0x6d6
@SuppressWarnings({"unused", "SameParameterValue"})
public final class oidb_0x6d6 {
    public static final class DeleteFileReqBody extends MessageMicro<DeleteFileReqBody> { }

    public static final class DeleteFileRspBody extends MessageMicro<DeleteFileRspBody> {}

    public static final class DownloadFileReqBody extends MessageMicro<DownloadFileReqBody> { }

    public static final class DownloadFileRspBody extends MessageMicro<DownloadFileRspBody> { }

    public static final class MoveFileReqBody extends MessageMicro<MoveFileReqBody> { }

    public static final class MoveFileRspBody extends MessageMicro<MoveFileRspBody> { }

    public static final class RenameFileReqBody extends MessageMicro<RenameFileReqBody> {
        static final MessageMicro.FieldMap __fieldMap__ = MessageMicro.initFieldMap(new int[]{8, 16, 24, 34, 42, 50}, new String[]{"uint64_group_code", "uint32_app_id", "uint32_bus_id", "str_file_id", "str_parent_folder_id", "str_new_file_name"}, new Object[]{0L, 0, 0, "", "", ""}, RenameFileReqBody.class);
        public final PBUInt64Field uint64_group_code = PBField.initUInt64(0);
        public final PBUInt32Field uint32_app_id = PBField.initUInt32(0);
        public final PBUInt32Field uint32_bus_id = PBField.initUInt32(0);
        public final PBStringField str_file_id = PBField.initString("");
        public final PBStringField str_parent_folder_id = PBField.initString("");
        public final PBStringField str_new_file_name = PBField.initString("");
    }

    public static final class RenameFileRspBody extends MessageMicro<RenameFileRspBody> {
        static final MessageMicro.FieldMap __fieldMap__ = MessageMicro.initFieldMap(new int[]{8, 18, 26}, new String[]{"int32_ret_code", "str_ret_msg", "str_client_wording"}, new Object[]{0, "", ""}, RenameFileRspBody.class);
        public final PBInt32Field int32_ret_code = PBField.initInt32(0);
        public final PBStringField str_ret_msg = PBField.initString("");
        public final PBStringField str_client_wording = PBField.initString("");
    }

    public static final class ReqBody extends MessageMicro<ReqBody> {
        static final MessageMicro.FieldMap __fieldMap__ = MessageMicro.initFieldMap(new int[]{10, 18, 26, 34, 42, 50}, new String[]{"upload_file_req", "resend_file_req", "download_file_req", "delete_file_req", "rename_file_req", "move_file_req"}, new Object[]{null, null, null, null, null, null}, ReqBody.class);
        public UploadFileReqBody upload_file_req = new UploadFileReqBody();
        public ResendReqBody resend_file_req = new ResendReqBody();
        public DownloadFileReqBody download_file_req = new DownloadFileReqBody();
        public DeleteFileReqBody delete_file_req = new DeleteFileReqBody();
        public RenameFileReqBody rename_file_req = new RenameFileReqBody();
        public MoveFileReqBody move_file_req = new MoveFileReqBody();
    }

    public static final class ResendReqBody extends MessageMicro<ResendReqBody> {
        static final MessageMicro.FieldMap __fieldMap__ = MessageMicro.initFieldMap(new int[]{8, 16, 24, 34, 42}, new String[]{"uint64_group_code", "uint32_app_id", "uint32_bus_id", "str_file_id", "bytes_sha"}, new Object[]{0L, 0, 0, "", ByteStringMicro.EMPTY}, ResendReqBody.class);
        public final PBUInt64Field uint64_group_code = PBField.initUInt64(0);
        public final PBUInt32Field uint32_app_id = PBField.initUInt32(0);
        public final PBUInt32Field uint32_bus_id = PBField.initUInt32(0);
        public final PBStringField str_file_id = PBField.initString("");
        public final PBBytesField bytes_sha = PBField.initBytes(ByteStringMicro.EMPTY);
    }

    public static final class ResendRspBody extends MessageMicro<ResendRspBody> {
        static final MessageMicro.FieldMap __fieldMap__;
        public final PBBytesField bytes_check_key;
        public final PBBytesField bytes_file_key;
        public final PBInt32Field int32_ret_code = PBField.initInt32(0);
        public final PBStringField str_ret_msg = PBField.initString("");
        public final PBStringField str_client_wording = PBField.initString("");
        public final PBStringField str_upload_ip = PBField.initString("");

        static {
            ByteStringMicro byteStringMicro = ByteStringMicro.EMPTY;
            __fieldMap__ = MessageMicro.initFieldMap(new int[]{8, 18, 26, 34, 42, 50}, new String[]{"int32_ret_code", "str_ret_msg", "str_client_wording", "str_upload_ip", "bytes_file_key", "bytes_check_key"}, new Object[]{0, "", "", "", byteStringMicro, byteStringMicro}, ResendRspBody.class);
        }

        public ResendRspBody() {
            ByteStringMicro byteStringMicro = ByteStringMicro.EMPTY;
            this.bytes_file_key = PBField.initBytes(byteStringMicro);
            this.bytes_check_key = PBField.initBytes(byteStringMicro);
        }
    }

    public static final class RspBody extends MessageMicro<RspBody> {
        static final MessageMicro.FieldMap __fieldMap__ = MessageMicro.initFieldMap(new int[]{10, 18, 26, 34, 42, 50}, new String[]{"upload_file_rsp", "resend_file_rsp", "download_file_rsp", "delete_file_rsp", "rename_file_rsp", "move_file_rsp"}, new Object[]{null, null, null, null, null, null}, RspBody.class);
        public UploadFileRspBody upload_file_rsp = new UploadFileRspBody();
        public ResendRspBody resend_file_rsp = new ResendRspBody();
        public DownloadFileRspBody download_file_rsp = new DownloadFileRspBody();
        public DeleteFileRspBody delete_file_rsp = new DeleteFileRspBody();
        public RenameFileRspBody rename_file_rsp = new RenameFileRspBody();
        public MoveFileRspBody move_file_rsp = new MoveFileRspBody();
    }

    public static final class UploadFileReqBody extends MessageMicro<UploadFileReqBody> { }

    public static final class UploadFileRspBody extends MessageMicro<UploadFileRspBody> { }

    private oidb_0x6d6() {
    }
}
