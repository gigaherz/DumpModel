# DumpModel
A small client-only forge mod to dump models from items and blocks to .OBJ file format.



# Setup

## Outside Development Environments
Download the mod from the [releases](https://github.com/gigaherz/DumpModel/releases) page and place it in your mods folder.

## In Development Environments
Add the mod as a dependency.

```gradle
repositories {
    maven { url 'https://dogforce-games.com/maven' }
}
dependencies {
    runtimeOnly fg.deobf('gigaherz.dumpmodel:DumpModel-1.15.2:1.0')
}
```

# Usage
DumpModel has one single client-side command.
This command is NOT integrated into the command system, so it will have no suggestions, and show in red.
I'll trust you can type it correctly. ;)

```
/dumpmodel held                          Dumps the item currently held in the main hand.
           target                        Dumps the block currently targetted. This will include any existing model data, unlike the 'block' option.
           item <item>[<nbt>]            Dumps the given item (optionally with the given NBT tag).
           block <block>[<properties>]   Dumps the given blockstate
```