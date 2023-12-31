import java.util.ArrayList;

public class TLRPC {
    public static abstract class FileLocation extends TLObject {

        public int dc_id;
        public long volume_id;
        public int local_id;
        public long secret;
        public byte[] file_reference;
        public byte[] key;
        public byte[] iv;

        public static FileLocation TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            FileLocation result = null;
            switch (constructor) {
                case 0x7c596b46:
                    result = new TL_fileLocationUnavailable();
                    break;
                case 0x53d69076:
                    result = new TL_fileLocation_layer82();
                    break;
                case 0x91d11eb:
                    result = new TL_fileLocation_layer97();
                    break;
                case 0xbc7fc6cd:
                    result = new TL_fileLocationToBeDeprecated();
                    break;
                case 0x55555554:
                    result = new TL_fileEncryptedLocation();
                    break;
            }
            if (result == null && exception) {
                throw new RuntimeException(String.format("can't parse magic %x in FileLocation", constructor));
            }
            if (result != null) {
                result.readParams(stream, exception);
            }
            return result;
        }
    }

    public static class TL_fileLocationUnavailable extends FileLocation {
        public static final int constructor = 0x7c596b46;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            volume_id = stream.readInt64(exception);
            local_id = stream.readInt32(exception);
            secret = stream.readInt64(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(volume_id);
            stream.writeInt32(local_id);
            stream.writeInt64(secret);
        }
    }

    public static class TL_fileLocation_layer82 extends TL_fileLocation_layer97 {
        public static final int constructor = 0x53d69076;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            dc_id = stream.readInt32(exception);
            volume_id = stream.readInt64(exception);
            local_id = stream.readInt32(exception);
            secret = stream.readInt64(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(dc_id);
            stream.writeInt64(volume_id);
            stream.writeInt32(local_id);
            stream.writeInt64(secret);
        }
    }

    public static class TL_fileLocation_layer97 extends FileLocation {
        public static final int constructor = 0x91d11eb;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            dc_id = stream.readInt32(exception);
            volume_id = stream.readInt64(exception);
            local_id = stream.readInt32(exception);
            secret = stream.readInt64(exception);
            file_reference = stream.readByteArray(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(dc_id);
            stream.writeInt64(volume_id);
            stream.writeInt32(local_id);
            stream.writeInt64(secret);
            stream.writeByteArray(file_reference);
        }
    }

    public static class TL_fileLocationToBeDeprecated extends FileLocation {
        public static final int constructor = 0xbc7fc6cd;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            volume_id = stream.readInt64(exception);
            local_id = stream.readInt32(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(volume_id);
            stream.writeInt32(local_id);
        }
    }

    public static class TL_fileEncryptedLocation extends FileLocation {
        public static final int constructor = 0x55555554;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            dc_id = stream.readInt32(exception);
            volume_id = stream.readInt64(exception);
            local_id = stream.readInt32(exception);
            secret = stream.readInt64(exception);
            key = stream.readByteArray(exception);
            iv = stream.readByteArray(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(dc_id);
            stream.writeInt64(volume_id);
            stream.writeInt32(local_id);
            stream.writeInt64(secret);
            stream.writeByteArray(key);
            stream.writeByteArray(iv);
        }
    }
    public static class TL_photoPathSize extends PhotoSize {
        public static final int constructor = 0xd8214d41;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            type = stream.readString(exception);
            bytes = stream.readByteArray(exception);
            w = h = 50;
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(type);
            stream.writeByteArray(bytes);
        }
    }

    public static class TL_photoSize extends PhotoSize {
        public static final int constructor = 0x75c78e60;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            type = stream.readString(exception);
            w = stream.readInt32(exception);
            h = stream.readInt32(exception);
            size = stream.readInt32(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(type);
            stream.writeInt32(w);
            stream.writeInt32(h);
            stream.writeInt32(size);
        }
    }
    public static abstract class PhotoSize extends TLObject {
        public String type;
        public FileLocation location;
        public int w;
        public int h;
        public int size;
        public byte[] bytes;

        public int gradientTopColor, gradientBottomColor; //custom

        public static PhotoSize TLdeserialize(long photo_id, long document_id, long sticker_set_id, AbstractSerializedData stream, int constructor, boolean exception) {
            PhotoSize result = null;
            switch (constructor) {
                case 0xd8214d41:
                    result = new TL_photoPathSize();
                    break;
                case 0x75c78e60:
                    result = new TL_photoSize();
                    break;
            }
            if (result == null && exception) {
                throw new RuntimeException(String.format("can't parse magic %x in PhotoSize", constructor));
            }
            if (result != null) {
                result.readParams(stream, exception);
                if (result.location == null) {
                    if (result.type.length() != 0 && (photo_id != 0 || document_id != 0 || sticker_set_id != 0)) {
                        result.location = new TL_fileLocationToBeDeprecated();
                        if (photo_id != 0) {
                            result.location.volume_id = -photo_id;
                            result.location.local_id = result.type.charAt(0);
                        } else if (document_id != 0) {
                            result.location.volume_id = -document_id;
                            result.location.local_id = 1000 + result.type.charAt(0);
                        } else if (sticker_set_id != 0) {
                            result.location.volume_id = -sticker_set_id;
                            result.location.local_id = 2000 + result.type.charAt(0);
                        }
                    } else {
                        result.location = new TL_fileLocationUnavailable();
                    }
                }
            }
            return result;
        }
    }

    public static abstract class StickerSet extends TLObject {

        public int flags;
        public boolean installed;
        public boolean archived;
        public boolean official;
        public boolean animated;
        public boolean masks;
        public boolean videos;
        public boolean emojis;
        public boolean text_color;
        public long id;
        public long access_hash;
        public String title;
        public String short_name;
        public int count;
        public int hash;
        public int installed_date;
        public ArrayList<PhotoSize> thumbs = new ArrayList<>();
        public int thumb_dc_id;
        public int thumb_version;
        public long thumb_document_id;
        public boolean gifs;

        public static StickerSet TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            StickerSet result = null;
            switch (constructor) {
                case 0x2dd14edc:
                    result = new TL_stickerSet();
                    break;
            }
            if (result == null && exception) {
                throw new RuntimeException(String.format("can't parse magic %x in StickerSet", constructor));
            }
            if (result != null) {
                result.readParams(stream, exception);
            }
            return result;
        }
    }

    public static class TL_stickerSet extends StickerSet {
        public static final int constructor = 0x2dd14edc;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            archived = (flags & 2) != 0;
            official = (flags & 4) != 0;
            masks = (flags & 8) != 0;
            animated = (flags & 32) != 0;
            videos = (flags & 64) != 0;
            emojis = (flags & 128) != 0;
            text_color = (flags & 512) != 0;
            if ((flags & 1) != 0) {
                installed_date = stream.readInt32(exception);
            }
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            title = stream.readString(exception);
            short_name = stream.readString(exception);
            if ((flags & 16) != 0) {
                int magic = stream.readInt32(exception);
                if (magic != 0x1cb5c415) {
                    if (exception) {
                        throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                    }
                    return;
                }
                int count = stream.readInt32(exception);
                for (int a = 0; a < count; a++) {
                    PhotoSize object = PhotoSize.TLdeserialize(0, 0, id, stream, stream.readInt32(exception), exception);
                    if (object == null) {
                        return;
                    }
                    thumbs.add(object);
                }
            }
            if ((flags & 16) != 0) {
                thumb_dc_id = stream.readInt32(exception);
            }
            if ((flags & 16) != 0) {
                thumb_version = stream.readInt32(exception);
            }
            if ((flags & 256) != 0) {
                thumb_document_id = stream.readInt64(exception);
            }
            count = stream.readInt32(exception);
            hash = stream.readInt32(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            flags = archived ? (flags | 2) : (flags &~ 2);
            flags = official ? (flags | 4) : (flags &~ 4);
            flags = masks ? (flags | 8) : (flags &~ 8);
            flags = animated ? (flags | 32) : (flags &~ 32);
            flags = videos ? (flags | 64) : (flags &~ 64);
            flags = emojis ? (flags | 128) : (flags &~ 128);
            flags = text_color ? (flags | 512) : (flags &~ 512);
            stream.writeInt32(flags);
            if ((flags & 1) != 0) {
                stream.writeInt32(installed_date);
            }
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeString(title);
            stream.writeString(short_name);
            if ((flags & 16) != 0) {
                stream.writeInt32(0x1cb5c415);
                int count = thumbs.size();
                stream.writeInt32(count);
                for (int a = 0; a < count; a++) {
                    thumbs.get(a).serializeToStream(stream);
                }
            }
            if ((flags & 16) != 0) {
                stream.writeInt32(thumb_dc_id);
            }
            if ((flags & 16) != 0) {
                stream.writeInt32(thumb_version);
            }
            if ((flags & 256) != 0) {
                stream.writeInt64(thumb_document_id);
            }
            stream.writeInt32(count);
            stream.writeInt32(hash);
        }
    }


    public static class TL_stickerPack extends TLObject {
        public static final int constructor = 0x12b299d4;

        public String emoticon;
        public ArrayList<Long> documents = new ArrayList<>();

        public static TL_stickerPack TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            if (TL_stickerPack.constructor != constructor) {
                if (exception) {
                    throw new RuntimeException(String.format("can't parse magic %x in TL_stickerPack", constructor));
                } else {
                    return null;
                }
            }
            TL_stickerPack result = new TL_stickerPack();
            result.readParams(stream, exception);
            return result;
        }

        public void readParams(AbstractSerializedData stream, boolean exception) {
            emoticon = stream.readString(exception);
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                documents.add(stream.readInt64(exception));
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(emoticon);
            stream.writeInt32(0x1cb5c415);
            int count = documents.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                stream.writeInt64(documents.get(a));
            }
        }
    }
    public static class TL_stickerKeyword extends TLObject {
        public static final int constructor = 0xfcfeb29c;

        public long document_id;
        public ArrayList<String> keyword = new ArrayList<>();

        public static TL_stickerKeyword TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            if (TL_stickerKeyword.constructor != constructor) {
                if (exception) {
                    throw new RuntimeException(String.format("can't parse magic %x in TL_stickerKeyword", constructor));
                } else {
                    return null;
                }
            }
            TL_stickerKeyword result = new TL_stickerKeyword();
            result.readParams(stream, exception);
            return result;
        }

        public void readParams(AbstractSerializedData stream, boolean exception) {
            document_id = stream.readInt64(exception);
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                keyword.add(stream.readString(exception));
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(document_id);
            stream.writeInt32(0x1cb5c415);
            int count = keyword.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                stream.writeString(keyword.get(a));
            }
        }
    }
    public static class TL_documentAttributeImageSize extends DocumentAttribute {
        public static final int constructor = 0x6c37c15c;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            w = stream.readInt32(exception);
            h = stream.readInt32(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(w);
            stream.writeInt32(h);
        }
    }
    public static class TL_documentEmpty extends Document {
        public static final int constructor = 0x36f8c871;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
        }
    }

    public static abstract class InputStickerSet extends TLObject {

        public long id;
        public long access_hash;
        public String short_name;

        public static InputStickerSet TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            InputStickerSet result = null;
            switch (constructor) {
                case 0xffb62b95:
                    result = new TL_inputStickerSetEmpty();
                    break;
                case 0x9de7a269:
                    result = new TL_inputStickerSetID();
                    break;
                case 0x861cc8a0:
                    result = new TL_inputStickerSetShortName();
                    break;
                case 0x28703c8:
                    result = new TL_inputStickerSetAnimatedEmoji();
                    break;
                case 0xe67f520e:
                    result = new TL_inputStickerSetDice();
                    break;
                case 0xc88b3b02:
                    result = new TL_inputStickerSetPremiumGifts();
                    break;
                case 0x29d0f5ee:
                    result = new TL_inputStickerSetEmojiDefaultStatuses();
                    break;
                case 0x4c4d4ce:
                    result = new TL_inputStickerSetEmojiGenericAnimations();
                    break;
                case 0x44c1f8e9:
                    result = new TL_inputStickerSetEmojiDefaultTopicIcons();
                    break;
            }
            if (result == null && exception) {
                throw new RuntimeException(String.format("can't parse magic %x in InputStickerSet", constructor));
            }
            if (result != null) {
                result.readParams(stream, exception);
            }
            return result;
        }
    }
    public static class TL_inputStickerSetPremiumGifts extends InputStickerSet {
        public static final int constructor = 0xc88b3b02;


        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputStickerSetEmojiDefaultStatuses extends InputStickerSet {
        public static final int constructor = 0x29d0f5ee;


        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputStickerSetDice extends InputStickerSet {
        public static final int constructor = 0xe67f520e;

        public String emoticon;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            emoticon = stream.readString(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(emoticon);
        }
    }

    public static class TL_inputStickerSetEmpty extends InputStickerSet {
        public static final int constructor = 0xffb62b95;


        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputStickerSetID extends InputStickerSet {
        public static final int constructor = 0x9de7a269;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
        }
    }

    public static class TL_inputStickerSetShortName extends InputStickerSet {
        public static final int constructor = 0x861cc8a0;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            short_name = stream.readString(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(short_name);
        }
    }

    public static class TL_inputStickerSetAnimatedEmoji extends InputStickerSet {
        public static final int constructor = 0x28703c8;


        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputStickerSetEmojiGenericAnimations extends InputStickerSet {
        public static final int constructor = 0x4c4d4ce;


        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }
    public static class TL_maskCoords extends TLObject {
        public static final int constructor = 0xaed6dbb2;

        public int n;
        public double x;
        public double y;
        public double zoom;

        public static TL_maskCoords TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            if (TL_maskCoords.constructor != constructor) {
                if (exception) {
                    throw new RuntimeException(String.format("can't parse magic %x in TL_maskCoords", constructor));
                } else {
                    return null;
                }
            }
            TL_maskCoords result = new TL_maskCoords();
            result.readParams(stream, exception);
            return result;
        }

        public void readParams(AbstractSerializedData stream, boolean exception) {
            n = stream.readInt32(exception);
            x = stream.readDouble(exception);
            y = stream.readDouble(exception);
            zoom = stream.readDouble(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(n);
            stream.writeDouble(x);
            stream.writeDouble(y);
            stream.writeDouble(zoom);
        }
    }
    public static class TL_inputStickerSetEmojiDefaultTopicIcons extends InputStickerSet {
        public static final int constructor = 0x44c1f8e9;


        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }


    public static class TL_documentAttributeSticker extends DocumentAttribute {
        public static final int constructor = 0x6319d612;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            mask = (flags & 2) != 0;
            alt = stream.readString(exception);
            stickerset = InputStickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            if ((flags & 1) != 0) {
                mask_coords = TL_maskCoords.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            flags = mask ? (flags | 2) : (flags &~ 2);
            stream.writeInt32(flags);
            stream.writeString(alt);
            stickerset.serializeToStream(stream);
            if ((flags & 1) != 0) {
                mask_coords.serializeToStream(stream);
            }
        }
    }
    public static class TL_documentAttributeFilename extends DocumentAttribute {
        public static final int constructor = 0x15590068;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            file_name = stream.readString(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(file_name);
        }
    }
    public static class TL_documentAttributeCustomEmoji extends DocumentAttribute {
        public static final int constructor = 0xfd149899;

        public boolean free;
        public boolean text_color;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            free = (flags & 1) != 0;
            text_color = (flags & 2) != 0;
            alt = stream.readString(exception);
            stickerset = InputStickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            flags = free ? (flags | 1) : (flags &~ 1);
            flags = text_color ? (flags | 2) : (flags &~ 2);
            stream.writeInt32(flags);
            stream.writeString(alt);
            stickerset.serializeToStream(stream);
        }
    }

    public static abstract class DocumentAttribute extends TLObject {
        public String alt;
        public InputStickerSet stickerset;
        public double duration;
        public int flags;
        public TL_maskCoords mask_coords;
        public boolean round_message;
        public boolean supports_streaming;
        public String file_name;
        public int w;
        public int h;
        public boolean mask;
        public String title;
        public String performer;
        public boolean voice;
        public byte[] waveform;
        public int preload_prefix_size;
        public boolean nosound;

        public static DocumentAttribute TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            DocumentAttribute result = null;
            switch (constructor) {
//                case 0x3a556302:
//                    result = new TL_documentAttributeSticker_layer55();
//                    break;
//                case 0xef02ce6:
//                    result = new TL_documentAttributeVideo_layer159();
//                    break;
//                case 0x51448e5:
//                    result = new TL_documentAttributeAudio_old();
//                    break;
                case 0x6319d612:
                    result = new TL_documentAttributeSticker();
                    break;
//                case 0x11b58939:
//                    result = new TL_documentAttributeAnimated();
//                    break;
                case 0x15590068:
                    result = new TL_documentAttributeFilename();
                    break;
//                case 0xd38ff1c2:
//                    result = new TL_documentAttributeVideo();
//                    break;
//                case 0x5910cccb:
//                    result = new TL_documentAttributeVideo_layer65();
//                    break;
//                case 0xded218e0:
//                    result = new TL_documentAttributeAudio_layer45();
//                    break;
//                case 0xfb0a5727:
//                    result = new TL_documentAttributeSticker_old();
//                    break;
//                case 0x9801d2f7:
//                    result = new TL_documentAttributeHasStickers();
//                    break;
//                case 0x994c9882:
//                    result = new TL_documentAttributeSticker_old2();
//                    break;
                case 0x6c37c15c:
                    result = new TL_documentAttributeImageSize();
                    break;
//                case 0x9852f9c6:
//                    result = new TL_documentAttributeAudio();
//                    break;
                case 0xfd149899:
                    result = new TL_documentAttributeCustomEmoji();
                    break;
            }
            if (result == null && exception) {
                throw new RuntimeException(String.format("can't parse magic %x in DocumentAttribute", constructor));
            }
            if (result != null) {
                result.readParams(stream, exception);
            }
            return result;
        }
    }

    public static class TL_document extends Document {
        public static final int constructor = 0x8fd4c4d8;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            file_reference = stream.readByteArray(exception);
            date = stream.readInt32(exception);
            mime_type = stream.readString(exception);
            size = stream.readInt64(exception);
            if ((flags & 1) != 0) {
                int magic = stream.readInt32(exception);
                if (magic != 0x1cb5c415) {
                    if (exception) {
                        throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                    }
                    return;
                }
                int count = stream.readInt32(exception);
                for (int a = 0; a < count; a++) {
                    PhotoSize object = PhotoSize.TLdeserialize(0, id, 0, stream, stream.readInt32(exception), exception);
                    if (object == null) {
                        return;
                    }
                    thumbs.add(object);
                }
            }
            if ((flags & 2) != 0) {
                int magic = stream.readInt32(exception);
                if (magic != 0x1cb5c415) {
                    if (exception) {
                        throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                    }
                    return;
                }
                int count = stream.readInt32(exception);
                for (int a = 0; a < count; a++) {
//                    VideoSize object = VideoSize.TLdeserialize(0, id, stream, stream.readInt32(exception), exception);
//                    if (object == null) {
//                        return;
//                    }
//                    video_thumbs.add(object);
                }
            }
            dc_id = stream.readInt32(exception);
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                DocumentAttribute object = DocumentAttribute.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                attributes.add(object);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeByteArray(file_reference);
            stream.writeInt32(date);
            stream.writeString(mime_type);
            stream.writeInt64(size);
            if ((flags & 1) != 0) {
                stream.writeInt32(0x1cb5c415);
                int count = thumbs.size();
                stream.writeInt32(count);
                for (int a = 0; a < count; a++) {
                    thumbs.get(a).serializeToStream(stream);
                }
            }
//            if ((flags & 2) != 0) {
//                stream.writeInt32(0x1cb5c415);
//                int count = video_thumbs.size();
//                stream.writeInt32(count);
//                for (int a = 0; a < count; a++) {
////                    video_thumbs.get(a).serializeToStream(stream);
//                }
//            }
            stream.writeInt32(dc_id);
            stream.writeInt32(0x1cb5c415);
            int count = attributes.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                attributes.get(a).serializeToStream(stream);
            }
        }
    }

    public static class TL_documentEncrypted extends Document {
        public static final int constructor = 0x55555558;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            date = stream.readInt32(exception);
            mime_type = stream.readString(exception);
            size = stream.readInt32(exception);
            thumbs.add(PhotoSize.TLdeserialize(0, 0, 0, stream, stream.readInt32(exception), exception));
            dc_id = stream.readInt32(exception);
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                DocumentAttribute object = DocumentAttribute.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                attributes.add(object);
            }
            key = stream.readByteArray(exception);
            iv = stream.readByteArray(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32(date);
            stream.writeString(mime_type);
            stream.writeInt32((int) size);
            thumbs.get(0).serializeToStream(stream);
            stream.writeInt32(dc_id);
            stream.writeInt32(0x1cb5c415);
            int count = attributes.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                attributes.get(a).serializeToStream(stream);
            }
            stream.writeByteArray(key);
            stream.writeByteArray(iv);
        }
    }

    public static abstract class Document extends TLObject {
        public int flags;
        public long id;
        public long access_hash;
        public byte[] file_reference;
        public long user_id;
        public int date;
        public String file_name;
        public String mime_type;
        public long size;
        public ArrayList<PhotoSize> thumbs = new ArrayList<>();
//        public ArrayList<VideoSize> video_thumbs = new ArrayList<>();
        public int version;
        public int dc_id;
        public byte[] key;
        public byte[] iv;
        public ArrayList<DocumentAttribute> attributes = new ArrayList<>();
        public String file_name_fixed; //custom
        public String localPath; //custom

        public static Document TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            Document result = null;
            switch (constructor) {
//                case 0x9ba29cc1:
//                    result = new TL_document_layer113();
//                    break;
//                case 0x59534e4c:
//                    result = new TL_document_layer92();
//                    break;
//                case 0x87232bc7:
//                    result = new TL_document_layer82();
//                    break;
//                case 0x55555556:
//                    result = new TL_documentEncrypted_old();
//                    break;
                case 0x8fd4c4d8:
                    result = new TL_document();
                    break;
//                case 0x1e87342b:
//                    result = new TL_document_layer142();
//                    break;
//                case 0x9efc6326:
//                    result = new TL_document_old();
//                    break;
                case 0x36f8c871:
                    result = new TL_documentEmpty();
                    break;
                case 0x55555558:
                    result = new TL_documentEncrypted();
                    break;
//                case 0xf9a39f4f:
//                    result = new TL_document_layer53();
//                    break;
            }
            if (result == null && exception) {
                throw new RuntimeException(String.format("can't parse magic %x in Document", constructor));
            }
            if (result != null) {
                result.readParams(stream, exception);
               // result.file_name_fixed = FileLoader.getDocumentFileName(result);
            }
            return result;
        }
    }

    public static abstract class messages_StickerSet extends TLObject {

        public StickerSet set;
        public ArrayList<TL_stickerPack> packs = new ArrayList<>();
        public ArrayList<TL_stickerKeyword> keywords = new ArrayList<>();
        public ArrayList<Document> documents = new ArrayList<>();

        public static TL_messages_stickerSet TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            TL_messages_stickerSet result = null;
            switch (constructor) {
                case 0x6e153f16:
                    result = new TL_messages_stickerSet();
                    break;
            }
            if (result == null && exception) {
                throw new RuntimeException(String.format("can't parse magic %x in messages_StickerSet", constructor));
            }
            if (result != null) {
                result.readParams(stream, exception);
            }
            return result;
        }
    }


    public static class TL_messages_stickerSet extends messages_StickerSet {
        public static final int constructor = 0x6e153f16;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            set = StickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                TL_stickerPack object = TL_stickerPack.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                packs.add(object);
            }
            magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                TL_stickerKeyword object = TL_stickerKeyword.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                keywords.add(object);
            }
            magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                Document object = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                documents.add(object);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            set.serializeToStream(stream);
            stream.writeInt32(0x1cb5c415);
            int count = packs.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                packs.get(a).serializeToStream(stream);
            }
            stream.writeInt32(0x1cb5c415);
            count = keywords.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                keywords.get(a).serializeToStream(stream);
            }
            stream.writeInt32(0x1cb5c415);
            count = documents.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                documents.get(a).serializeToStream(stream);
            }
        }
    }

}
