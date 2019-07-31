[![Logo](https://github.com/SeeSharpSoft/intellij-file-preview/blob/master/src/main/resources/META-INF/pluginIcon.svg)](https://plugins.jetbrains.com/plugin/12778-file-preview) 
[![Plugin version](https://img.shields.io/jetbrains/plugin/d/12778-file-preview.svg)](https://plugins.jetbrains.com/plugin/12778-file-preview)
[![Build Status](https://travis-ci.org/SeeSharpSoft/intellij-file-preview.svg?branch=master)](https://travis-ci.org/SeeSharpSoft/intellij-file-preview)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c37dba93cf5a4d46a61e0f570be245fb)](https://www.codacy.com/app/github_124/intellij-file-preview?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=SeeSharpSoft/intellij-file-preview&amp;utm_campaign=Badge_Grade)
[![Coverage Status](https://coveralls.io/repos/github/SeeSharpSoft/intellij-file-preview/badge.svg?branch=master)](https://coveralls.io/github/SeeSharpSoft/intellij-file-preview?branch=master)

# Quick File Preview (IntelliJ plugin)

Compatible with _IDEA  PhpStorm  WebStorm  PyCharm  RubyMine  AppCode  CLion  Gogland  DataGrip  Rider  MPS  Android Studio_

This plugin enables a quick file preview on simple selecting files in Project View - similar to the preview in Sublime or VSCode.

**Features:**

- show preview (open temporary editor) of selected file
- focus editor of selected file if already opened

This plugin is fresh like a mild breeze in summer - please contribute issues, ideas and feedback. Thanks!

(inspired by https://youtrack.jetbrains.com/issue/IDEA-130918)

## Installation

Install it from the Jetbrains plugin repository within your IDE (**recommended**):

- _File > Settings > Plugins > Browse repositories... > Search 'File Preview' > Category 'Editor'_

You can also download the JAR package from the [Jetbrains plugin repository](https://plugins.jetbrains.com/plugin/12778-file-preview) or from [GitHub Releases](https://github.com/SeeSharpSoft/intellij-file-preview/releases) and add it manually to your plugins:

- _File > Settings > Plugins > Install plugin from disk..._

## Build & Run from source code

Clone this repository (https://github.com/SeeSharpSoft/intellij-file-preview.git).

Build the plugin:

```
gradle build
```
    
Start IDE:

```
gradle runIdea
```
