How to run GSN
==============

1 In Eclipse, create new java project, point the directory to the your gsn directory.

2 Right click on the build.xml, choose Run As, choose [2 Ant Build...]
There are a lot of check boxes. First, to build GSN, choose build (default).
And run it.

3 To start GSN, choose check box gsn, and run it. If you still have build
checked, uncheck it.

Note: If you use Java 1.7, you might experience some error when you run gsn.
You might want to use Java 1.6. However, if you have both Java 1.6 and Java 1.7
installed, you can solve this by ask GSN to choose Java 1.6:
- on build.xml, locate these two lines:
    <target name="build" depends="setup" description="Compile the cource code.">
    <javac debug="true" srcdir="${src.dir}" optimize="off"
- modify the second line above to:
    <javac debug="true" srcdir="${src.dir}" source="1.6" target="1.6" optimize="off"
- run clean (and leave anything else unchecked)
- run build (and leave anything else unchecked)
- run gsn (and leave anything else unchecked)


