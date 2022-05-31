// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// ignore_for_file: public_member_api_docs

/// An example of using the plugin, controlling lifecycle and playback of the
/// video.

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';

void main() {
  runApp(
    MaterialApp(
      home: _App(),
    ),
  );
}

class _App extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 3,
      child: Scaffold(
        key: const ValueKey<String>('home_page'),
        appBar: AppBar(
          title: const Text('Video player example'),
          actions: <Widget>[
            IconButton(
              key: const ValueKey<String>('push_tab'),
              icon: const Icon(Icons.navigation),
              onPressed: () {
                Navigator.push<_PlayerVideoAndPopPage>(
                  context,
                  MaterialPageRoute<_PlayerVideoAndPopPage>(
                    builder: (BuildContext context) => _PlayerVideoAndPopPage(),
                  ),
                );
              },
            )
          ],
          bottom: const TabBar(
            isScrollable: true,
            tabs: <Widget>[
              Tab(
                icon: Icon(Icons.cloud),
                text: "Remote",
              ),
              Tab(icon: Icon(Icons.insert_drive_file), text: "Asset"),
              Tab(icon: Icon(Icons.list), text: "List example"),
            ],
          ),
        ),
        body: TabBarView(
          children: <Widget>[
            _BumbleBeeRemoteVideo(),
            _ButterFlyAssetVideo(),
            _ButterFlyAssetVideoInList(),
          ],
        ),
      ),
    );
  }
}

class _ButterFlyAssetVideoInList extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return ListView(
      children: <Widget>[
        _ExampleCard(title: "Item a"),
        _ExampleCard(title: "Item b"),
        _ExampleCard(title: "Item c"),
        _ExampleCard(title: "Item d"),
        _ExampleCard(title: "Item e"),
        _ExampleCard(title: "Item f"),
        _ExampleCard(title: "Item g"),
        Card(
            child: Column(children: <Widget>[
          Column(
            children: <Widget>[
              const ListTile(
                leading: Icon(Icons.cake),
                title: Text("Video video"),
              ),
              Stack(alignment: FractionalOffset.bottomRight + const FractionalOffset(-0.1, -0.1), children: <Widget>[
                _ButterFlyAssetVideo(),
                Image.asset('assets/flutter-mark-square-64.png'),
              ]),
            ],
          ),
        ])),
        _ExampleCard(title: "Item h"),
        _ExampleCard(title: "Item i"),
        _ExampleCard(title: "Item j"),
        _ExampleCard(title: "Item k"),
        _ExampleCard(title: "Item l"),
      ],
    );
  }
}

/// A filler card to show the video in a list of scrolling contents.
class _ExampleCard extends StatelessWidget {
  const _ExampleCard({Key? key, required this.title}) : super(key: key);

  final String title;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          ListTile(
            leading: const Icon(Icons.airline_seat_flat_angled),
            title: Text(title),
          ),
          ButtonBar(
            children: <Widget>[
              TextButton(
                child: const Text('BUY TICKETS'),
                onPressed: () {
                  /* ... */
                },
              ),
              TextButton(
                child: const Text('SELL TICKETS'),
                onPressed: () {
                  /* ... */
                },
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _ButterFlyAssetVideo extends StatefulWidget {
  @override
  _ButterFlyAssetVideoState createState() => _ButterFlyAssetVideoState();
}

class _ButterFlyAssetVideoState extends State<_ButterFlyAssetVideo> {
  late VideoPlayerController _controller;

  @override
  void initState() {
    super.initState();
    _controller = VideoPlayerController.asset('assets/Butterfly-209.mp4');

    _controller.addListener(() {
      setState(() {});
    });
    _controller.setLooping(true);
    _controller.initialize().then((_) => setState(() {}));
    _controller.play();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      child: Column(
        children: <Widget>[
          Container(
            padding: const EdgeInsets.only(top: 20.0),
          ),
          const Text('With assets mp4'),
          Container(
            padding: const EdgeInsets.all(20),
            child: AspectRatio(
              aspectRatio: _controller.value.aspectRatio,
              child: Stack(
                alignment: Alignment.bottomCenter,
                children: <Widget>[
                  VideoPlayer(_controller),
                  _ControlsOverlay(controller: _controller),
                  VideoProgressIndicator(_controller, allowScrubbing: true),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _BumbleBeeRemoteVideo extends StatefulWidget {
  @override
  _BumbleBeeRemoteVideoState createState() => _BumbleBeeRemoteVideoState();
}

class _BumbleBeeRemoteVideoState extends State<_BumbleBeeRemoteVideo> {
  late VideoPlayerController _controller;

  Future<ClosedCaptionFile> _loadCaptions() async {
    final String fileContents = await DefaultAssetBundle.of(context).loadString('assets/bumble_bee_captions.vtt');
    return WebVTTCaptionFile(fileContents); // For vtt files, use WebVTTCaptionFile
  }

  @override
  void initState() {
    super.initState();
    _controller = VideoPlayerController.network(
      // 'https://flutter.github.io/assets-for-api-docs/assets/videos/bee.mp4',
      // "http://res.uquabc.com/video/encrpyt_hls/butterfly.m3u8",
      // "https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8",
      "https://vedio-pro.iwordnet.com/9cfafb52be1941cbaf208f2c4fd8707a/video/ff0dece6c75e48a2a0e44fed9660f116-55f7f877238f5f53fd6f1677cfb4204f-video-sd.m3u8",
      closedCaptionFile: _loadCaptions(),
      videoPlayerOptions: VideoPlayerOptions(mixWithOthers: true),
    );

    _controller.addListener(() {
      setState(() {});
    });
    _controller.setLooping(true);
    _controller.initialize();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      child: Column(
        children: <Widget>[
          Container(padding: const EdgeInsets.only(top: 20.0)),
          const Text('With remote mp4'),
          Container(
            padding: const EdgeInsets.all(20),
            child: AspectRatio(
              aspectRatio: _controller.value.aspectRatio,
              child: Stack(
                alignment: Alignment.bottomCenter,
                children: <Widget>[
                  VideoPlayer(_controller),
                  ClosedCaption(text: _controller.value.caption.text),
                  _ControlsOverlay(controller: _controller),
                  VideoProgressIndicator(_controller, allowScrubbing: true),
                ],
              ),
            ),
          ),
          CacheWidget(_controller)
        ],
      ),
    );
  }
}

class _ControlsOverlay extends StatelessWidget {
  const _ControlsOverlay({Key? key, required this.controller}) : super(key: key);

  static const _exampleCaptionOffsets = [
    Duration(seconds: -10),
    Duration(seconds: -3),
    Duration(seconds: -1, milliseconds: -500),
    Duration(milliseconds: -250),
    Duration(milliseconds: 0),
    Duration(milliseconds: 250),
    Duration(seconds: 1, milliseconds: 500),
    Duration(seconds: 3),
    Duration(seconds: 10),
  ];
  static const _examplePlaybackRates = [
    0.25,
    0.5,
    1.0,
    1.5,
    2.0,
    3.0,
    5.0,
    10.0,
  ];

  final VideoPlayerController controller;

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: <Widget>[
        AnimatedSwitcher(
          duration: Duration(milliseconds: 50),
          reverseDuration: Duration(milliseconds: 200),
          child: controller.value.isPlaying
              ? SizedBox.shrink()
              : Container(
                  color: Colors.black26,
                  child: Center(
                    child: Icon(
                      Icons.play_arrow,
                      color: Colors.white,
                      size: 100.0,
                      semanticLabel: 'Play',
                    ),
                  ),
                ),
        ),
        GestureDetector(
          onTap: () {
            controller.value.isPlaying ? controller.pause() : controller.play();
          },
        ),
        Align(
          alignment: Alignment.topLeft,
          child: PopupMenuButton<Duration>(
            initialValue: controller.value.captionOffset,
            tooltip: 'Caption Offset',
            onSelected: (delay) {
              controller.setCaptionOffset(delay);
            },
            itemBuilder: (context) {
              return [
                for (final offsetDuration in _exampleCaptionOffsets)
                  PopupMenuItem(
                    value: offsetDuration,
                    child: Text('${offsetDuration.inMilliseconds}ms'),
                  )
              ];
            },
            child: Padding(
              padding: const EdgeInsets.symmetric(
                // Using less vertical padding as the text is also longer
                // horizontally, so it feels like it would need more spacing
                // horizontally (matching the aspect ratio of the video).
                vertical: 12,
                horizontal: 16,
              ),
              child: Text('${controller.value.captionOffset.inMilliseconds}ms'),
            ),
          ),
        ),
        Align(
          alignment: Alignment.topRight,
          child: PopupMenuButton<double>(
            initialValue: controller.value.playbackSpeed,
            tooltip: 'Playback speed',
            onSelected: (speed) {
              controller.setPlaybackSpeed(speed);
            },
            itemBuilder: (context) {
              return [
                for (final speed in _examplePlaybackRates)
                  PopupMenuItem(
                    value: speed,
                    child: Text('${speed}x'),
                  )
              ];
            },
            child: Padding(
              padding: const EdgeInsets.symmetric(
                // Using less vertical padding as the text is also longer
                // horizontally, so it feels like it would need more spacing
                // horizontally (matching the aspect ratio of the video).
                vertical: 12,
                horizontal: 16,
              ),
              child: Text('${controller.value.playbackSpeed}x'),
            ),
          ),
        ),
      ],
    );
  }
}

class _PlayerVideoAndPopPage extends StatefulWidget {
  @override
  _PlayerVideoAndPopPageState createState() => _PlayerVideoAndPopPageState();
}

class _PlayerVideoAndPopPageState extends State<_PlayerVideoAndPopPage> {
  late VideoPlayerController _videoPlayerController;
  bool startedPlaying = false;

  @override
  void initState() {
    super.initState();

    _videoPlayerController = VideoPlayerController.asset('assets/Butterfly-209.mp4');
    _videoPlayerController.addListener(() {
      if (startedPlaying && !_videoPlayerController.value.isPlaying) {
        Navigator.pop(context);
      }
    });
  }

  @override
  void dispose() {
    _videoPlayerController.dispose();
    super.dispose();
  }

  Future<bool> started() async {
    await _videoPlayerController.initialize();
    await _videoPlayerController.play();
    startedPlaying = true;
    return true;
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      elevation: 0,
      child: Center(
        child: FutureBuilder<bool>(
          future: started(),
          builder: (BuildContext context, AsyncSnapshot<bool> snapshot) {
            if (snapshot.data == true) {
              return AspectRatio(
                aspectRatio: _videoPlayerController.value.aspectRatio,
                child: VideoPlayer(_videoPlayerController),
              );
            } else {
              return const Text('waiting for video to load');
            }
          },
        ),
      ),
    );
  }
}

class CacheWidget extends StatelessWidget {
  VideoPlayerController videoPlayerController;

  CacheWidget(this.videoPlayerController);

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder(
      valueListenable: videoPlayerController.downloadNotifier,
      builder: (context, DownloadState? downloadState, Widget? child) {
        if (downloadState?.state == DownloadState.DOWNLOADING) {
          return _downloadingWidget(downloadState!);
        } else if (downloadState?.state == DownloadState.COMPLETED) {
          return _downloadComplete(videoPlayerController, downloadState!);
        } else if (downloadState?.state == DownloadState.ERROR) {
          return _downloadError(videoPlayerController, downloadState!);
        } else {
          return _noDownloadWidget(videoPlayerController);
        }
      },
    );
  }
}

///下载中
Widget _downloadingWidget(DownloadState downloadState) {
  return Column(
    children: <Widget>[
      LinearProgressIndicator(
        value: (downloadState.progress ?? 0) * 0.01,
      )
    ],
  );
}

///下载完成
Widget _downloadComplete(VideoPlayerController videoPlayerController, DownloadState downloadState) {
  return Column(
    children: [
      Text('已缓存'),
      TextButton.icon(
          onPressed: () {
            videoPlayerController.removeDownload();
          },
          icon: Icon(Icons.delete),
          label: Text('删除缓存'))
    ],
  );
}

///下载error
Widget _downloadError(VideoPlayerController videoPlayerController, DownloadState downloadState) {
  return Column(
    children: [
      Text('出错啦'),
      TextButton.icon(
          onPressed: () {
            videoPlayerController.removeDownload();
          },
          icon: Icon(Icons.delete),
          label: Text('删除缓存'))
    ],
  );
  return Text('出错啦');
}

Widget _noDownloadWidget(VideoPlayerController videoPlayerController) {
  return Column(
    children: [
      Text('未缓存'),
      TextButton.icon(
          onPressed: () {
            print('===>点击下载');
            videoPlayerController.download("下载测试");
          },
          icon: Icon(Icons.download),
          label: Text('开始缓存'))
    ],
  );
}
