//
//  ZBLM3u8Manager.h
//  M3U8DownLoadTest
//
//  Created by zengbailiang on 10/4/17.
//  Copyright © 2017 controling. All rights reserved.
//

#import <Foundation/Foundation.h>
typedef void (^ZBLM3u8ManagerDownloadSuccessBlock)(NSString *localPlayUrlString);
typedef void (^ZBLM3u8ManagerDownloadFaildBlock)(NSString *url, NSError * error);
typedef void (^ZBLM3u8ManagerDownloadProgressHandler)(float progress);

@protocol ZBLM3u8ManagerDownloadDelegate <NSObject>

- (void)m3u8DownloadSuccess:(NSString *)normalDownloadUrl;

- (void)m3u8DownloadFailed:(NSString *)normalDownloadUrl error:(NSError *)error;

- (void)m3u8Downloading:(NSString *)normalDownloadUrl progress:(float)progress;

@end

typedef enum : NSUInteger {
    UNDOWNLOAD = 0,
    DOWNLOADING = 1,
    COMPLETED = 2,
    ERROR = 3
} GpDownloadState;

@interface ZBLM3u8Manager : NSObject

@property (nonatomic, weak)id<ZBLM3u8ManagerDownloadDelegate>delegate;

+ (instancetype)shareInstance;

- (BOOL)exitLocalVideoWithUrlString:(NSString*) urlStr;

- (BOOL)downloadingUrl:(NSArray *)urls;

- (NSString *)localPlayUrlWithOriUrlString:(NSString *)urlString;

- (void)startDownloadUrl:(NSString *)url;

- (void)downloadVideoWithUrlString:(NSString *)urlStr downloadProgressHandler:(ZBLM3u8ManagerDownloadProgressHandler)downloadProgressHandler downloadSuccessBlock:(ZBLM3u8ManagerDownloadSuccessBlock) downloadSuccessBlock downloadFaildBlock:(ZBLM3u8ManagerDownloadFaildBlock) downloadFaildBlock;

- (void)tryStartLocalService;

- (void)tryStopLocalService;

- (void)resumeDownload;
- (void)suspendDownload;

- (void)removeAllM3u8Cache;
@end
