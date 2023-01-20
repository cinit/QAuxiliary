## Description
Decode silk v3 audio files (like wechat amr, aud files, qq slk files) and convert to other format (like mp3).
Batch conversion support.

<a href="https://github.com/kn007/silk-v3-decoder/blob/master/LICENSE"><img src="https://img.shields.io/badge/license-MIT-green.svg?style=flat"></a>

```
silk-v3-decoder            (Decode Silk V3 Audio Files)
  |
  |---  silk               (Skype Silk Codec)
  |
  |---  windows            (For Windows Platform Users Program)
  |
  |---  LICENSE            (License)
  |
  |---  README.md          (Readme)
  |
  |---  converter.sh       (Converter Shell Script)
  |
  |---  converter_beta.sh  (Converter Shell Script(Beta))
```

## Requirement

* gcc
* ffmpeg

## How To Use

```
sh converter.sh silk_v3_file/input_folder output_format/output_folder flag(format)
```
E.g., convert a file:
```
sh converter.sh 33921FF3774A773BB193B6FD4AD7C33E.slk mp3
```
Notice: the `33921FF3774A773BB193B6FD4AD7C33E.slk` is an audio file you need to convert, the `mp3` is a format you need to output.

If you need to convert all audio files in one folder, now batch conversion support, using like this:
```
sh converter.sh input ouput mp3
```
Notice: the `input` folder is content the audio files you need to convert, the `output` folder is content the audio files after conversion finished, the `mp3` is a format you need to output.

If you need to convert files on the `Windows` platfrom, [click here](https://dl.kn007.net/directlink/silk2mp3.zip "silk2mp3.zip") to download zip package for `silk2mp3.exe` to convert, also can <a href='/windows' target="_blank">click here</a> to get more information.

## Other

Also provide silk v3 encode codec, compatible with Wechat/QQ.

## About

[kn007's blog](https://kn007.net) 

***

## 中文说明
解码silk v3音频文件（类似微信的amr和aud文件、QQ的slk文件）并转换为其它格式（如MP3）。
支持批量转换。

<a href="https://github.com/kn007/silk-v3-decoder/blob/master/LICENSE"><img src="https://img.shields.io/badge/license-MIT-green.svg?style=flat"></a>

```
silk-v3-decoder            (解码silk v3音频文件)
  |
  |---  silk               (Skype Silk源码)
  |
  |---  windows            (可用于Windows平台的应用程序)
  |
  |---  LICENSE            (软件使用范围许可)
  |
  |---  README.md          (说明)
  |
  |---  converter.sh       (转换脚本)
  |
  |---  converter_beta.sh  (转换脚本(测试版))
```

## 依赖组件

* gcc
* ffmpeg

## 如何使用

```
sh converter.sh silk_v3_file/input_folder output_format/output_folder flag(format)
```
比如转换一个文件，使用：
```
sh converter.sh 33921FF3774A773BB193B6FD4AD7C33E.slk mp3
```
注意：其中`33921FF3774A773BB193B6FD4AD7C33E.slk`是要转换的文件，而`mp3`是最终转换后输出的格式。

如果你需要批量转换，比如转换某个目录，那么使用：
```
sh converter.sh input ouput mp3
```
注意：其中`input`是要转换的目录，而`output`是最终转换后音频输出的目录，最后的`mp3`参数是最终转换后输出的格式。

如果你需要在`Windows`下使用该程序，请下载[silk2mp3.exe](https://dl.kn007.net/directlink/silk2mp3.zip "silk2mp3.zip")应用程序来完成转换，你可<a href='/windows' target="_blank">点击这里</a>来查看更多Windows下如何使用的相关说明。

## 其他说明

如果你需要对音频文件进行silk v3编码，源码也已经提供，并且对微信、QQ进行了兼容，详见参数。

## 关于作者

[kn007的个人博客](https://kn007.net) 
