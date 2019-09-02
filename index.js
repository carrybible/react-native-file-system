/**
 * React Native File System
 * @flow
 */

import { NativeModules } from 'react-native'

const { RNFileSystem } = NativeModules

const normalizeFilePath = (path: string) => (path.startsWith('file://') ? path.slice(7) : path)

type MkdirOptions = {
  NSURLIsExcludedFromBackupKey?: boolean, // iOS only
  NSFileProtectionKey?: string, // IOS only
}

type FileOptions = {
  NSFileProtectionKey?: string, // IOS only
}

const RNFS = {
  exists(filepath: string): Promise<boolean> {
    return RNFileSystem.exists(normalizeFilePath(filepath))
  },

  mkdir(filepath: string, options: MkdirOptions = {}): Promise<void> {
    return RNFileSystem.mkdir(normalizeFilePath(filepath), options).then(() => void 0)
  },

  writeFile(
    filePath: string,
    data: string,
    encoding?: string = 'utf8',
    append?: boolean = false,
  ): Promise<string> {
    return RNFileSystem.writeFile(filePath, encoding, data, append)
  },

  appendFile(filePath: string, data: string, encoding?: string = 'utf8'): Promise<string> {
    return RNFileSystem.writeFile(filePath, encoding, data, true)
  },

  readFile(filepath: string, encoding?: string = 'utf8'): Promise<string> {
    return RNFileSystem.readFile(filepath, encoding)
  },

  read(
    filepath: string,
    length: number = 0,
    position: number = 0,
    encodingOrOptions?: any,
  ): Promise<string> {
    let options = {
      encoding: 'utf8',
    }

    if (encodingOrOptions) {
      if (typeof encodingOrOptions === 'string') {
        options.encoding = encodingOrOptions
      } else if (typeof encodingOrOptions === 'object') {
        options = encodingOrOptions
      }
    }

    return RNFileSystem.read(normalizeFilePath(filepath), length, position).then(utf8 => {
      return utf8
    })
  },

  copyFile(filepath: string, destPath: string, options?: FileOptions = {}): Promise<void> {
    return RNFileSystem.copyFile(
      normalizeFilePath(filepath),
      normalizeFilePath(destPath),
      options,
    ).then(() => void 0)
  },

  Dir: {
    MainBundle: RNFileSystem.RNFSMainBundlePath,
    Caches: RNFileSystem.RNFSCachesDirectoryPath,
    ExternalCaches: RNFileSystem.RNFSExternalCachesDirectoryPath,
    Document: RNFileSystem.RNFSDocumentDirectoryPath,
    External: RNFileSystem.RNFSExternalDirectoryPath,
    ExternalStorage: RNFileSystem.RNFSExternalStorageDirectoryPath,
    Temporary: RNFileSystem.RNFSTemporaryDirectoryPath,
    Library: RNFileSystem.RNFSLibraryDirectoryPath,
    Pictures: RNFileSystem.RNFSPicturesDirectoryPath,
  },

  FileProtectionKeys: RNFileSystem.RNFSFileProtectionKeys,
}

export default RNFS
