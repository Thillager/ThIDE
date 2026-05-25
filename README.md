# TIDE - Java IDE

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-100%25-orange)](https://www.java.com/)
[![Version](https://img.shields.io/badge/Version-latest-blue)](https://github.com/Thillager/TIDE/releases/latest)

TIDE is a **lightweight, user-friendly Java IDE (Integrated Development Environment)** developed specifically for Java development. It provides all essential features for Java programming, but also for other languages, like batch, python, c or c++, but java is the best supported one.

## Features

- ✅ **Project Management** - Easy management of Java projects
- ✅ **Code Editor** - Syntax highlighting and code editing
- ✅ **Compiler Integration** - Direct compilation of Java code
- ✅ **Lightweight** - Fast performance and low resource consumption
- ✅ **Platform Independent** - Runs on Windows, Linux and macOS (all Java-supporting systems)
- ✅ **GUI with Java Swing** - Native and responsive user interface
- ✅ **Update Button** - No annoying reinstalls for new versions, no automatic updates with potential malware
- ✅ **Multiple languages** - Not only one language supported

## Requirements

- **Java development kit (JDK) 25 or higher**
     - Or use the .msi/.deb installers, then the jdk version is irrelevant
- **At least 750 MB RAM**
- **50 MB free disk space**

## Installation and Usage

### Option 1: Installer

#### Linux:

1. Download the .deb file from the latest release.

2. Install:
   ```bash
   sudo apt install ./filename.deb
   ```

#### Windows:

1. Download the .msi or .exe (version dependent) installer from the latest release

2. Run by double-clicking.

### Option 2: Run Pre-compiled JAR

1. Make sure Java is installed on your system:
   ```bash
   java -version
   ```

2. Run the JAR file:
   ```bash
   java -jar TIDE.jar
   ```

3. The TIDE IDE will open and be ready to use.

## How TIDE Works

### Workflow Example

1. **Create Project**: Start TIDE and create a new project via the menu of TBuild
2. **Write Code**: Write your Java classes in the integrated editor
3. **Compile**: Use the Build button or menu to compile
4. **Run**: Execute your program directly from TIDE
5. **Debug**: Check compiler errors in the console

## Project Structure

```
TIDE/
├── src/                      # Source code
│   └── main/
│       └── java/             # Java source files
├── libs/                      # External libraries
├── production/                # Production artifacts
├── T.xml                      # Project configuration
```

## Configuration

The `T.xml` file contains the project configuration:

```xml
<project>
  <mainClass>TIDE</mainClass>      <!-- Main class to run -->
  <appName>TIDE</appName>          <!-- Application name -->
  <version>1.0.0</version>         <!-- Version string -->
</project>
```

You can edit this file to set various configurations for your project.
Or you can use TBuild, which can be downloaded and run via the TBuild button and can edit the T.xml graphically.

## Example: Your First Program with TIDE

### Step 1: Create a New Project
Start TIDE and go to a new folder. Then Press TBuild and in TBuild Initialize project.

### Step 2: Create a New Java Class
Go to src/main/java/Main.java

### Step 3: Write Code
```java
public class Main {
    public static void main(String[] args) {
        System.out.println("Welcome to TIDE!");
    }
}
```

### Step 4: Compile and Run
- Click the **Run** button

## Updates

### Frequency
- Updates come whenever I have time, ideas, or bugs to fix

### How do I install them?
- Start TIDE as administrator
- Click the "About" button
- Click the "Check for Updates" button
- Install
- Wait a moment (until the desktop icon reloads)
- Start

## Troubleshooting

### Problem: "Java not found"
**Solution**: Install Java Runtime Environment (JRE) from [java.com](https://www.java.com)

### Problem: JAR file won't run
**Solution**:
```bash
# Check Java version
java -version

# Run with explicit path
java -jar /path/to/TIDE.jar
```

### Problem: Compiler errors despite correct code
**Solution**:
- Check your Java syntax
- Make sure all classes are in the correct directories
- Check the error output in the console

## Documentation and Links

- **Java Documentation**: https://docs.oracle.com/en/java/
- **GitHub Repository**: https://github.com/Thillager/TIDE
- **TBuild**:
https://github.com/Thillager/Tbuild

## License

This project is licensed under the **MIT License**. See [LICENSE](LICENSE) for details.
This project uses dependencies. The necessary licenses are in the THIRD_PARTY_LICENSES.md

## Built With
TIDE uses the power of proven open-source libraries:
* **[JGit](https://www.eclipse.org/jgit/)** - For integrated version control.
* **[RSyntaxTextArea](https://github.com/bobbylight/RSyntaxTextArea)** - For syntax highlighting in the editor.
* **[Autocomplete](https://github.com/bobbylight/AutoComplete)** - For automatic intelligent code completion.
* **[FlatLaf](https://www.formdev.com/flatlaf/)** - For the modern, flat design of the user interface.

## Contributing

Contributions are welcome! To contribute:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Questions and Support

If you have questions or issues:
- Open a [GitHub Issue](https://github.com/Thillager/TIDE/issues)
- Check existing issues for frequently asked questions

---
**Maintainer:** [@Thillager](https://github.com/Thillager)

Good luck programming with TIDE!
