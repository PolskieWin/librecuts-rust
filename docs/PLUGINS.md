# LibreCut RS Plugin Development Guide

This guide explains how to design, package, and distribute custom plugins for **LibreCut RS**.

LibreCut RS includes a modular, 100% offline plugin architecture. Plugins can modify the user interface layout, add creative video filters and effects, introduce specialized audio DSP presets, and automate editing workflows.

---

## 1. Plugin Architecture Overview

Plugins in LibreCut RS are packaged as standalone `.lcplugin` or `.json` files. They require no compilation, no external SDKs, and execute entirely on-device without internet access.

### Supported Plugin Categories

| Category | Identifier | Description |
|---|---|---|
| **UI Layout** | `UI_LAYOUT` | Replaces or reconfigures the timeline interface, button docks, and action bars (e.g. CapCut Pro, Alight Motion, Minimalist). |
| **Video Filter & Effect** | `EFFECT` | Introduces color LUTs, motion blurs, distortion shaders, noise overlays, and artistic looks. |
| **Audio Processing** | `AUDIO` | Equalizer curves, vocal clarity boosters, bass enhancement, and sound design presets. |
| **Editing Tool** | `TOOL` | Automated cutting scripts, beat detection alignment, and timeline macros. |

---

## 2. Plugin File Specification

Every plugin is defined by a JSON manifest. The file extension can be either `.lcplugin` or `.json`.

### Manifest Schema

```json
{
  "id": "com.example.myplugin",
  "name": "My Custom Plugin",
  "version": "1.0.0",
  "author": "Your Name / Organization",
  "description": "Clear explanation of what the plugin does.",
  "category": "UI_LAYOUT",
  "enabled": false,
  "parameters": [
    {
      "key": "param_key",
      "label": "Parameter Name",
      "type": "SLIDER",
      "defaultValue": 0.5,
      "minValue": 0.0,
      "maxValue": 1.0,
      "options": []
    }
  ]
}
```

### Parameter Types

| Type | Description | Applicable Fields |
|---|---|---|
| `SLIDER` | Continuous numeric adjustment | `defaultValue`, `minValue`, `maxValue` |
| `TOGGLE` | Boolean on/off switch | `defaultValue` (0.0 for false, 1.0 for true) |
| `DROPDOWN` | Single selection from predefined list | `defaultValue`, `options` array |

---

## 3. Tutorial: Creating a Custom UI Layout Plugin

UI Layout plugins change how LibreCut RS arranges preview players, track headers, action ribbons, and quick-access pills.

### Example: "Desktop Pro" Studio Layout

Create a file named `studio_pro_layout.lcplugin`:

```json
{
  "id": "org.community.ui.studio_pro",
  "name": "Studio Pro Layout",
  "version": "1.0.0",
  "author": "Community UX Lab",
  "description": "Expands the multi-track timeline, docks precision frame stepping beside the timecode, and surfaces quick split and keyframe tools.",
  "category": "UI_LAYOUT",
  "enabled": false,
  "parameters": [
    {
      "key": "timeline_height_ratio",
      "label": "Timeline Viewport Height",
      "type": "SLIDER",
      "defaultValue": 0.65,
      "minValue": 0.40,
      "maxValue": 0.85,
      "options": []
    },
    {
      "key": "show_quick_split_pill",
      "label": "Floating Quick Action Pill",
      "type": "TOGGLE",
      "defaultValue": 1.0,
      "minValue": 0.0,
      "maxValue": 1.0,
      "options": []
    }
  ]
}
```

### Behavior in LibreCut RS

When enabled in **Top Bar -> Plugins Manager**:
- Only one `UI_LAYOUT` plugin can be active at a time.
- Enabling `Studio Pro Layout` automatically deactivates other layout plugins (such as CapCut Pro or Alight Motion).
- The timeline dynamically adjusts track density and floating controls according to the parameters.

---

## 4. Tutorial: Creating a Custom Filter / Effect Plugin

Effect plugins define color transformations, blur kernels, or creative distortions.

### Example: "Dreamy Motion Glow" Filter Plugin

Create a file named `dreamy_motion_glow.lcplugin`:

```json
{
  "id": "org.community.fx.dreamy_glow",
  "name": "Dreamy Motion Glow",
  "version": "1.1.0",
  "author": "VulkanShader Dev",
  "description": "Combines soft directional motion blur with highlight diffusion and warm saturation boost for cinematic dream sequences.",
  "category": "EFFECT",
  "enabled": true,
  "parameters": [
    {
      "key": "blur_radius",
      "label": "Motion Blur Spread",
      "type": "SLIDER",
      "defaultValue": 0.45,
      "minValue": 0.0,
      "maxValue": 1.0,
      "options": []
    },
    {
      "key": "glow_intensity",
      "label": "Highlight Bloom",
      "type": "SLIDER",
      "defaultValue": 0.35,
      "minValue": 0.0,
      "maxValue": 1.0,
      "options": []
    },
    {
      "key": "warmth_tint",
      "label": "Warm Golden Tint",
      "type": "TOGGLE",
      "defaultValue": 1.0,
      "minValue": 0.0,
      "maxValue": 1.0,
      "options": []
    }
  ]
}
```

---

## 5. Installing and Testing Your Plugin

### Method 1: In-App Import via System File Picker

1. Transfer your `.lcplugin` or `.json` file to your Android device (via USB, local storage, or local file manager).
2. Open **LibreCut RS**.
3. Open any project or create a new project.
4. Tap the **Plugin Extension Icon** in the top navigation bar.
5. In the **Plugins Framework** sheet, tap **Import Plugin**.
6. Select your `.lcplugin` file from your device.
7. The plugin will appear in the installed list with its parameters, ready to be enabled.

### Method 2: Manual Directory Placement (ADB / Root)

If developing via ADB:

```bash
# Push directly into the internal plugins directory
adb push my_filter.lcplugin /data/data/com.librecuts.rust/files/plugins/
```

Restart the app to load the new plugin automatically.

---

## 6. Community Submission Guidelines

To submit a plugin for inclusion in official community releases:

1. Ensure the manifest validates against standard JSON parsers.
2. Verify that no parameters have negative ranges unless required.
3. Keep descriptions concise, accurate, and completely free of emojis or promotional text.
4. Open a Pull Request on the GitHub repository: `https://github.com/PolskieWin/librecuts-rust` with your `.lcplugin` file added to `plugins/community/`.

