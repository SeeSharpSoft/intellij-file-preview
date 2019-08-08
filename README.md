[![Logo](https://github.com/SeeSharpSoft/intellij-file-preview/blob/master/src/main/resources/META-INF/pluginIcon.svg)](https://plugins.jetbrains.com/plugin/12778-file-preview) 
[![Plugin version](https://img.shields.io/jetbrains/plugin/d/12778-file-preview.svg)](https://plugins.jetbrains.com/plugin/12778-file-preview)
[![Build Status](https://travis-ci.org/SeeSharpSoft/intellij-file-preview.svg?branch=master)](https://travis-ci.org/SeeSharpSoft/intellij-file-preview)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c37dba93cf5a4d46a61e0f570be245fb)](https://www.codacy.com/app/github_124/intellij-file-preview?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=SeeSharpSoft/intellij-file-preview&amp;utm_campaign=Badge_Grade)
[![Coverage Status](https://coveralls.io/repos/github/SeeSharpSoft/intellij-file-preview/badge.svg?branch=master)](https://coveralls.io/github/SeeSharpSoft/intellij-file-preview?branch=master)
[![Donate](https://img.shields.io/badge/Paypal-Donate-yellow)](https://paypal.me/knerzbert)


# Quick File Preview (IntelliJ plugin)

Compatible with _IDEA  PhpStorm  WebStorm  PyCharm  RubyMine  AppCode  CLion  Gogland  DataGrip  Rider  MPS  Android Studio_

This plugin enables a quick file preview on simple selecting files in Project View - similar to the preview in Sublime or VSCode.

**Features:**

- show preview (open temporary editor) of selected file
- focus editor of selected file if already opened

**Please note:** If **[Autoscroll to source](https://www.jetbrains.com/help/idea/navigating-through-the-source-code.html#scroll_to_from_source)** is enabled, the actual editor is opened instead of a preview.
This is the intended and unchanged behavior. To make proper use of the Preview Tab, this option has to be disabled!

This plugin is fresh like a mild breeze in summer - please contribute issues, ideas and feedback. Thanks!

(inspired by https://youtrack.jetbrains.com/issue/IDEA-130918)

## Settings (*defaults*)

### Preview behavior

Defines when the Preview Editor tab should be shown in general. 

#### Auto preview on select *(default)*

Whenever the focused file in the Project View changes, a preview is shown or the already existing editor is focused. 

#### Manual preview

Whenever the focused file in the Project View changes, the already existing editor is focused. The Preview Editor tab is only shown when pressing \<SPACE>. The preview stays open on further navigation and must also be closed manually.

### Close Preview Editor tab if no file is selected (*activated*)

Switching the focus in the Project View to an directory or non-displayable element, the preview is closed. Deactivate to keep the last Preview Editor tab.

### Close Preview Editor tab if other tab is selected (*deactivated*)

Switching editor tabs does not close the current Preview Editor tab on default. 

### Enable quick navigation key events (*activated*)

Additional key commands are active if Project View is focused:

\<ESC>: close the currently focused editor

\<TAB>: move input focus to the currently selected file editor 

### Focus Project View after opening/closing file editor (*activated*)

Grab/keep focus on Project View (instead of focusing editor) when opening or closing a file.

### Open actual editor when editing preview (Preview Editor tab gets closed) (*activated*)

If activated, the Preview Editor tab is closed and the actual editor is opened when a change happened during preview. This causes a tiny input interruption. If deactivated, the Preview Editor tab stays open while editing, the actual editor is only opened on explicit \<DOUBLE-CLICK> or \<ENTER>.

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
