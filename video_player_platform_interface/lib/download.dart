import 'package:flutter/material.dart';

/// Created by cnting on 2022/5/30

class DownloadState {
  static const int UNDOWNLOAD = 0;
  static const int DOWNLOADING = 1;
  static const int COMPLETED = 2;
  static const int ERROR = 3;

  DownloadState(this.state, {this.progress = 0});

  final int state;
  final double? progress;
}

class DownloadNotifier extends ValueNotifier<DownloadState> {
  DownloadNotifier(DownloadState value) : super(value);
}