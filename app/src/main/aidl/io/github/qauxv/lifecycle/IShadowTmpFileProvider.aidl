// IShadowTmpFileProvider.aidl
package io.github.qauxv.lifecycle;

// Declare any non-default types here with import statements

interface IShadowTmpFileProvider {

    boolean isTmpFileExists(String id);

    long getTmpFileSize(String id);

    String getTmpFileMimeType(String id);

    String getTmpFileName(String id);

    ParcelFileDescriptor getTmpFileDescriptor(String id);

}
