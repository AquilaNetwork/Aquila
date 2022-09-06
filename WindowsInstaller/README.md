# Windows installer

## Prerequisites

* AdvancedInstaller v16 or better, and enterprise licence if translations are required
* Installed AdoptOpenJDK v11 64bit, full JDK *not* JRE

## General build instructions

If this is your first time opening the `aquila.aip` file then you might need to adjust
configured paths, or create a dummy `D:` drive with the expected layout.

Typical build procedure:

* Place the `aquila.jar` file in `Install-Files\`
* Open AdvancedInstaller with aquila.aip file
* If releasing a new version, change version number in:
	+ "Product Information" side menu
	+ "Product Details" side menu entry
	+ "Product Details" tab in "Product Details" pane
	+ "Product Version" entry box
* Click away to a different side menu entry, e.g. "Resources" -> "Files and Folders"
* You should be prompted whether to generate a new product key, click "Generate New"
* Click "Build" button
* New EXE should be generated in `Aquila-SetupFiles\` folder with correct version number

