# StegVI

> Tugas 2 II4021 Kriptografi 2026

<p align="center">
  <img src="https://media.tenor.com/s3mh97FyKoIAAAAM/bocchi-the-rock-hitori-gotoh.gif" width="300" alt="StegVI banner" />
</p>

<h3 align="center">Steganography for AVI and MP4 video with sequential/random embedding and A5/1 encryption</h3>

## Overview
`StegVI` is a desktop steganography application for hiding text or files inside video media. The main pipeline supports AVI-based visual embedding using LSB schemes (`3-3-2`, `3-2-3`, `2-3-3`) with sequential and random embedding modes. The project also includes MP4 pipeline that stores payload inside the MP4 container while preserving the video stream. The application provides embedding metrics such as MSE, PSNR, and averaged RGB histograms across frames.

## Tech Stack and Languages
<p align="center">
  <img src="https://raw.githubusercontent.com/Ender-Wiggin2019/ServiceLogos/main/Java/Java.png" alt="Java" width="250"/>
</p>

<p align="center">
  <a href="https://youtu.be/7FDRQifEMUQ?si=gKheP3GnBORXsDY4">Kessoku!</a>
  ·
  <a href="https://github.com/anisaalhaqi/II4021-Kriptografi-Tubes-1/releases/">Releases</a>
  ·
  <a href="https://github.com/anisaalhaqi/II4021-Kriptografi-Tubes-1/blob/main/LICENSE">License</a>
  ·
  <a href="https://github.com/anisaalhaqi/II4021-Kriptografi-Tubes-1/blob/main/doc/">Project Report</a>
</p>

## Authors
<div align="center">
  <table>
    <tr>
      <th>NIM</th>
      <th>Name</th>
      <th>GitHub</th>
    </tr>
    <tr align="center">
      <td>13523067</td>
      <td>Benedict Presley</td>
      <td>
        <a href="https://github.com/BP04">
          <img src="https://github.com/BP04.png" width="48" alt="BP04" /><br/>
          <sub><b>@BP04</b></sub>
        </a>
      </td>
    </tr>
    <tr align="center">
      <td>13523090</td>
      <td>Nayaka Ghana Subrata</td>
      <td>
        <a href="https://github.com/Nayekah">
          <img src="https://github.com/Nayekah.png" width="48" alt="Nayekah" /><br/>
          <sub><b>@Nayekah</b></sub>
        </a>
      </td>
    </tr>
    <tr align="center">
      <td>18224080</td>
      <td>Anisa Aulia Alhaqi</td>
      <td>
        <a href="https://github.com/anisaalhaqi">
          <img src="https://github.com/anisaalhaqi.png" width="48" alt="anisaalhaqi" /><br/>
          <sub><b>@anisaalhaqi</b></sub>
        </a>
      </td>
    </tr>
  </table>
</div>

## About StegVI
`StegVI` was built to explore practical video steganography on desktop. The AVI pipeline focuses on LSB-based embedding directly on video frames, while the MP4 bonus pipeline focuses on payload preservation inside the MP4 container. The application supports text and file payloads, optional A5/1 encryption, sequential and random embedding, and extraction with integrity-oriented handling. The GUI is implemented using JavaFX, while media processing is handled through JavaCV and FFmpeg.

---
## Features
- Embed text or files into AVI cover videos
- Extract hidden text or files from stego videos
- Three LSB schemes for AVI: `3-3-2`, `3-2-3`, and `2-3-3`
- Sequential and random embedding modes
- A5/1 encryption
- MSE and PSNR calculation
- Averaged RGB histogram visualization across frames
- MP4 cover video support

---
## Installation & Setup

### Requirements
- Git
- JDK 21 or newer
- Internet connection for the first Gradle dependency download

> [!IMPORTANT]
> Make sure `java -version` points to JDK 21+ before running the project.

### Dependencies
The project uses these main libraries and frameworks:
- JavaFX
- JavaCV
- FFmpeg

### How to Install
> [!TIP]
> If you're using Windows, installing JDK from Adoptium or Oracle JDK is the most straightforward option.  
> If you're using Linux, install OpenJDK 21 from your package manager.

1. Clone the repository:
```bash
git clone https://github.com/anisaalhaqi/II4021-Kriptografi-Tubes-1.git
```

2. Move into the project directory:
```bash
cd II4021-Kriptografi-Tubes-1
```

3. Verify Java:
```bash
java -version
```

4. Download project dependencies:
```bash
# Windows
.\gradlew.bat dependencies

# Linux / macOS
./gradlew dependencies
```

---
## How to Run
### GUI Application
1. Open a terminal in the project root.
2. Run:
```bash
# Windows
.\gradlew.bat run

# Linux / macOS
./gradlew run
```

The application entry point is:
- `com.steganography.ui.MainApp`

---
## Build
1. Open a terminal in the project root.
2. Run:
```bash
# Windows
.\gradlew.bat build

# Linux / macOS
./gradlew build
```

This will:
- compile the source code
- run the Gradle build lifecycle
- generate build outputs under `build/`

### Compile Only
```bash
# Windows
.\gradlew.bat compileJava

# Linux / macOS
./gradlew compileJava
```

### Clean Build Artifacts
```bash
# Windows
.\gradlew.bat clean

# Linux / macOS
./gradlew clean
```

---
## Supported Workflows
### AVI
- Cover video: AVI
- Embedding: frame-based LSB
- Encryption: A5/1
- Modes: sequential / random
- Schemes: `3-3-2`, `3-2-3`, `2-3-3`
- Metrics: MSE, PSNR, histogram

### MP4
- Cover video: MP4
- Embedding: MP4 container payload storage
- Encryption: A5/1
- Modes: sequential / random
- Video stream remains visually unchanged

---
## Contact

If you have questions, please contact the authors:

Benedict Presley <13523067@std.stei.itb.ac.id>  
Nayaka Ghana Subrata <13523090@std.stei.itb.ac.id>  
Anisa Aulia Alhaqi <18224080@std.stei.itb.ac.id>

---

<br/>
<br/>

<div align="center">
II4021 Kriptografi • 2026 • StegVI
</div>