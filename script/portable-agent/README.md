# FLASH-MAS Portable Agent

This directory contains the necessary scripts and configuration to run the FLASH-MAS Daemon as a portable agent. The agent can be run on different machines without requiring a full development environment.

## Prerequisites

1.  **Git LFS:** The bundled Java Runtime Environment (`jre` folder) is stored using Git Large File Storage (LFS). To ensure it is downloaded correctly, you need to have `git-lfs` installed on your machine. You can download it from [git-lfs.github.com](https://git-lfs.github.com/). After installation, run `git lfs pull` in the repository root to download the JRE.

2.  **Java Artifacts:** You need to build two JAR files (`FlashMasDaemon.jar` and `flash-node.jar`) from your IDE. Once built, copy them into this `portable-agent` directory. See the **Building the Artifacts** section below for instructions.

## Building the Artifacts (IntelliJ IDEA)

Since this project does not use a build system like Maven or Gradle, you must build the JAR artifacts directly from the IDE. Here is how to configure them in IntelliJ IDEA:

### 1. Build `FlashMasDaemon.jar` (Runnable Daemon)
1. Go to **File -> Project Structure... -> Artifacts**.
2. Click the **+** (Add) button -> **JAR -> From modules with dependencies...**
3. Select the `FLASH-MAS` module.
4. For the **Main Class**, browse and select `quick.FlashDaemon`.
5. Under **JAR files from libraries**, select **extract to the target JAR** (this ensures dependencies are bundled).
6. Set the **Name** to `FlashMasDaemon`.
7. Leave the default **Output directory** (usually `your-project-path/out/artifacts/FlashMasDaemon`).
8. Click **Apply** and **OK**.

### 2. Build `flash-node.jar` (Agent Node)
1. Go to **File -> Project Structure... -> Artifacts**.
2. Click the **+** (Add) button -> **JAR -> Empty**.
3. Name it `flash-node`.
4. Leave the default **Output directory** (usually `your-project-path/out/artifacts/flash_node`).
5. In the **Available Elements** panel on the right, double-click on `'FLASH-MAS' compile output` to add it to the JAR. You may also need to add any necessary library dependencies here depending on your node's logic.
6. Click **Apply** and **OK**.

### 3. Generate and Prepare the Agent
1. Go to **Build -> Build Artifacts...** in the top menu.
2. Select **All Artifacts** (or select them individually) and click **Build**.
3. Navigate to your project's `out/artifacts/` folder.
4. **Copy** both `FlashMasDaemon.jar` and `flash-node.jar`.
5. **Paste** them into this `script/portable-agent/` directory.

## Directory Structure

For the agent to work correctly, the `portable-agent` directory must have the following structure:

```
portable-agent/
├── FlashMasDaemon.jar      # The main daemon artifact (copied here)
├── flash-node.jar          # The node artifact spawned by the daemon (copied here)
├── basic-chat.yml          # Example deployment configuration
├── jre/                    # (Optional) Bundled Java Runtime Environment
│   └── ...
├── start.bat               # Start script for Windows
├── start.sh                # Start script for Linux/macOS
└── README.md               # This file
```

**Important:** The agent relies on relative paths to find its configuration files (like `basic-chat.yml`) and the bundled JRE. Therefore, you **must** execute the start scripts from within this `portable-agent` directory.

## Running the Agent

### Windows

Open a Command Prompt or PowerShell window, navigate to this directory, and run:

```cmd
cd path\to\portable-agent
.\start.bat
```

### Linux / macOS

Open a terminal, navigate to this directory, make the script executable, and run it:

```sh
cd path/to/portable-agent
chmod +x start.sh
./start.sh
```

### Command-Line Arguments

You can pass command-line arguments to the daemon through the script. For example, to specify a different port:

```sh
# On Linux/macOS
./start.sh -p 12345

# On Windows
.\start.bat -p 12345
```

## How It Works

The `start.bat` and `start.sh` scripts are designed to make the agent portable.

1.  They first check for the existence of a `./jre` directory.
2.  If a bundled JRE is found, they will use it to run the Java application. This ensures that the agent runs with a consistent Java version, regardless of what is installed on the host system.
3.  If the `./jre` directory is not found, the scripts will fall back to using the `java` command available in the system's `PATH`.