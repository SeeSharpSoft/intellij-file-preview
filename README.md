**PLEASE NOTE: This project is no longer maintained due to an added [built-in functionality in IntelliJ](https://www.jetbrains.com/help/idea/using-code-editor.html#preview-tab) to preview files, which works quite similar to this plugin.**

---

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
- settings page to individualize preview behavior
- customize the editor tab representation (title & color)
- **since 1.5.2** keep collapse/expand state in project view if parent node is collapsed (customizable, enabled by default)

**Please note:** If **[Autoscroll to source/Open files with single click](https://www.jetbrains.com/help/idea/navigating-through-the-source-code.html#scroll_to_from_source)** is enabled, the actual editor tab is opened instead of a preview. To make proper use of the preview tab, **this option must be disabled!**

(inspired by https://youtrack.jetbrains.com/issue/IDEA-130918)

## Settings (*defaults*)

### Project View

#### Preview behavior

Defines when the Preview Editor tab should be shown in general. 

##### Auto preview on select *(default)*

Whenever the focused file in the Project View changes, a preview is shown or the already existing editor is focused. 

##### Manual preview

Whenever the focused file in the Project View changes, the already existing editor is focused. The Preview Editor tab is only shown when pressing \<SPACE>. The preview stays open on further navigation and must also be closed manually.

#### Toggle tree expand/collapse by single click (*enabled*)

Defines the required number of clicks to expand/collapse a node in Project View tree: one click if option is *enabled* (plugin default), two clicks if option is *disabled* (IDE default).

#### Keep expand/collapse state (*enabled*)

If enabled, the expand/collapse states of the folders are kept when a parent folder gets collapsed/expanded.

#### Close preview if no file selected (*enabled*)

Switching the focus in the Project View to an directory or non-displayable element, the preview is closed. Disable to keep the last Preview Editor tab.

Please note: Due to [unwanted side effects](https://github.com/SeeSharpSoft/intellij-file-preview/issues/77), this setting has no effect if **[Always Select Opened File](https://www.jetbrains.com/help/idea/navigating-through-the-source-code.html#scroll_to_from_source)** is enabled.

### Preview Editor Tab

#### Tab title pattern (*<<%s>>*)

Defines how the file name is formatted in the preview tab title, while **%s** represents the filename. Leaving the field blank will show the title like for usual editor tabs. Tab coloring can be used to distinguish.

#### Tab color (*disabled*)

Sets a custom background color for the preview tab.

#### Close preview if other tab is selected (*disabled*)

Switching editor tabs does not close the current Preview Editor tab on default.

#### Convert to default editor tab when editing previewed content (*enabled*)

If activated, the tab becomes a normal editor tab when a change happened during preview, so it will stay open if another file gets selected.

**Please note:** A default editor tab can always be enforced by \<DOUBLE-CLICK> or \<ENTER> on the file in Project View.

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
