
#import "RNFileSystem.h"

@implementation RNFileSystem

- (dispatch_queue_t)methodQueue
{
  return dispatch_queue_create("studio.earthling.rnfs", DISPATCH_QUEUE_SERIAL);
}

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(readFile:(NSString *)filepath
                  encoding:(NSString *)encoding
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  BOOL fileExists = [[NSFileManager defaultManager] fileExistsAtPath:filepath];

  if (!fileExists) {
    return reject(@"ENOENT", [NSString stringWithFormat:@"ENOENT: no such file or directory, open '%@'", filepath], nil);
  }

  NSError *error = nil;

  NSDictionary *attributes = [[NSFileManager defaultManager] attributesOfItemAtPath:filepath error:&error];

  if (error) {
    return [self reject:reject withError:error];
  }

  if ([attributes objectForKey:NSFileType] == NSFileTypeDirectory) {
    return reject(@"EISDIR", @"EISDIR: illegal operation on a directory, read", nil);
  }

  NSData *content = [[NSFileManager defaultManager] contentsAtPath:filepath];

  if(encoding != nil)
  {
      if([[encoding lowercaseString] isEqualToString:@"utf8"])
      {
          NSString * utf8 = [[NSString alloc] initWithData:content encoding:NSUTF8StringEncoding];
          if(utf8 == nil)
              resolve([[NSString alloc] initWithData:content encoding:NSISOLatin1StringEncoding]);
          else
              resolve(utf8);
      }
      else if ([[encoding lowercaseString] isEqualToString:@"base64"]) {
          resolve([content base64EncodedStringWithOptions:0]);
      }
      else if ([[encoding lowercaseString] isEqualToString:@"ascii"]) {
          NSMutableArray * resultArray = [NSMutableArray array];
          char * bytes = [content bytes];
          for(int i=0;i<[content length];i++) {
              [resultArray addObject:[NSNumber numberWithChar:bytes[i]]];
          }
          resolve(resultArray);
      }
  }
  else
  {
      resolve(content);
  }
}

RCT_EXPORT_METHOD(read:(NSString *)filepath
                  length: (NSInteger *)length
                  position: (NSInteger *)position
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    BOOL fileExists = [[NSFileManager defaultManager] fileExistsAtPath:filepath];
    
    if (!fileExists) {
        return reject(@"ENOENT", [NSString stringWithFormat:@"ENOENT: no such file or directory, open '%@'", filepath], nil);
    }
    
    NSError *error = nil;
    
    NSDictionary *attributes = [[NSFileManager defaultManager] attributesOfItemAtPath:filepath error:&error];
    
    if (error) {
        return [self reject:reject withError:error];
    }
    
    if ([attributes objectForKey:NSFileType] == NSFileTypeDirectory) {
        return reject(@"EISDIR", @"EISDIR: illegal operation on a directory, read", nil);
    }
    
    // Open the file handler.
    NSFileHandle *file = [NSFileHandle fileHandleForReadingAtPath:filepath];
    if (file == nil) {
        return reject(@"EISDIR", @"EISDIR: Could not open file for reading", nil);
    }
    
    // Seek to the position if there is one.
    [file seekToFileOffset: (int)position];
    
    NSData *content;
    if ((int)length > 0) {
        content = [file readDataOfLength: (int)length];
    } else {
        content = [file readDataToEndOfFile];
    }
    
    NSString * utf8 = [[NSString alloc] initWithData:content encoding:NSUTF8StringEncoding];
    
    resolve(utf8);
}

RCT_EXPORT_METHOD(copyFile:(NSString *)filepath
                  destPath:(NSString *)destPath
                  options:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  NSFileManager *manager = [NSFileManager defaultManager];

  NSError *error = nil;
  BOOL success = [manager copyItemAtPath:filepath toPath:destPath error:&error];

  if (!success) {
    return [self reject:reject withError:error];
  }

  if ([options objectForKey:@"NSFileProtectionKey"]) {
    NSMutableDictionary *attributes = [[NSMutableDictionary alloc] init];
    [attributes setValue:[options objectForKey:@"NSFileProtectionKey"] forKey:@"NSFileProtectionKey"];
    BOOL updateSuccess = [manager setAttributes:attributes ofItemAtPath:destPath error:&error];

    if (!updateSuccess) {
      return [self reject:reject withError:error];
    }
  }

  resolve(nil);
}

# pragma mark - mkdir

RCT_EXPORT_METHOD(mkdir:(NSString *)filepath
                  options:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  NSFileManager *manager = [NSFileManager defaultManager];

  NSMutableDictionary *attributes = [[NSMutableDictionary alloc] init];

  if ([options objectForKey:@"NSFileProtectionKey"]) {
      [attributes setValue:[options objectForKey:@"NSFileProtectionKey"] forKey:@"NSFileProtectionKey"];
  }

  NSError *error = nil;
    BOOL success = [manager createDirectoryAtPath:filepath withIntermediateDirectories:YES attributes:attributes error:&error];

  if (!success) {
    return [self reject:reject withError:error];
  }

  NSURL *url = [NSURL fileURLWithPath:filepath];

  if ([[options allKeys] containsObject:@"NSURLIsExcludedFromBackupKey"]) {
    NSNumber *value = options[@"NSURLIsExcludedFromBackupKey"];
    success = [url setResourceValue: value forKey: NSURLIsExcludedFromBackupKey error: &error];

    if (!success) {
      return [self reject:reject withError:error];
    }
  }

  resolve(nil);
}

# pragma mark - exist

RCT_EXPORT_METHOD(exists:(NSString *)filepath
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(__unused RCTPromiseRejectBlock)reject)
{
  BOOL fileExists = [[NSFileManager defaultManager] fileExistsAtPath:filepath];

  resolve([NSNumber numberWithBool:fileExists]);
}

# pragma mark - write file

RCT_EXPORT_METHOD(writeFile:(NSString *)path
                  encoding:(NSString *)encoding
                  data:(NSString *)data
                  append:(BOOL)append
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    @try {
        NSFileManager * fm = [NSFileManager defaultManager];
        NSError * err = nil;
        // check if the folder exists, if it does not exist create folders recursively
        // after the folders created, write data into the file
        NSString * folder = [path stringByDeletingLastPathComponent];
        encoding = [encoding lowercaseString];

        BOOL isDir = NO;
        BOOL exists = NO;
        exists = [fm fileExistsAtPath:path isDirectory: &isDir];

        if (isDir) {
            return reject(@"EISDIR", [NSString stringWithFormat:@"Expecting a file but '%@' is a directory", path], nil);
        }

        if(!exists) {
            [fm createDirectoryAtPath:folder withIntermediateDirectories:YES attributes:NULL error:&err];
            if(err != nil) {
                return reject(@"ENOTDIR", [NSString stringWithFormat:@"Failed to create parent directory of '%@'; error: %@", path, [err description]], nil);
            }
            if(![fm createFileAtPath:path contents:nil attributes:nil]) {
                return reject(@"ENOENT", [NSString stringWithFormat:@"File '%@' does not exist and could not be created", path], nil);
            }
        }

        NSFileHandle *fileHandle = [NSFileHandle fileHandleForWritingAtPath:path];
        NSData * content = nil;
        if([encoding containsString:@"base64"]) {
            content = [[NSData alloc] initWithBase64EncodedString:data options:0];
        }
        else if([encoding isEqualToString:@"uri"]) {
            return;
        }
        else {
            content = [data dataUsingEncoding:NSUTF8StringEncoding];
        }
        if(append == YES) {
            [fileHandle seekToEndOfFile];
            [fileHandle writeData:content];
            [fileHandle closeFile];
        }
        else {
            [content writeToFile:path atomically:YES];
        }
        fm = nil;

        resolve([NSNumber numberWithInteger:[content length]]);
    }
    @catch (NSException * e)
    {
        reject(@"EUNSPECIFIED", [e description], nil);
    }
}


- (void)reject:(RCTPromiseRejectBlock)reject withError:(NSError *)error
{
  NSString *codeWithDomain = [NSString stringWithFormat:@"E%@%zd", error.domain.uppercaseString, error.code];
  reject(codeWithDomain, error.localizedDescription, error);
}

- (NSString *)getPathForDirectory:(int)directory
{
  NSArray *paths = NSSearchPathForDirectoriesInDomains(directory, NSUserDomainMask, YES);
  return [paths firstObject];
}

- (NSDictionary *)constantsToExport
{
  return @{
    @"RNFSMainBundlePath": [[NSBundle mainBundle] bundlePath],
    @"RNFSCachesDirectoryPath": [self getPathForDirectory:NSCachesDirectory],
    @"RNFSDocumentDirectoryPath": [self getPathForDirectory:NSDocumentDirectory],
    @"RNFSExternalDirectoryPath": [NSNull null],
    @"RNFSExternalStorageDirectoryPath": [NSNull null],
    @"RNFSTemporaryDirectoryPath": NSTemporaryDirectory(),
    @"RNFSLibraryDirectoryPath": [self getPathForDirectory:NSLibraryDirectory],
    @"RNFSFileTypeRegular": NSFileTypeRegular,
    @"RNFSFileTypeDirectory": NSFileTypeDirectory,
    @"RNFSFileProtectionComplete": NSFileProtectionComplete,
    @"RNFSFileProtectionCompleteUnlessOpen": NSFileProtectionCompleteUnlessOpen,
    @"RNFSFileProtectionCompleteUntilFirstUserAuthentication": NSFileProtectionCompleteUntilFirstUserAuthentication,
    @"RNFSFileProtectionNone": NSFileProtectionNone
  };
}

@end
