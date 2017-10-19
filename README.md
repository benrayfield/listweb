# listweb
versioned mindmap of dragAndDroppable named lists of lists... in a web for people who have thousands of thoughts to organize and only a few seconds here and there to do it

<nobr>
  <img src="https://raw.githubusercontent.com/benrayfield/listweb/master/images/listweb_0.1_example_jobListWebsites.png"/>
  <img src="https://raw.githubusercontent.com/benrayfield/listweb/master/images/listweb_0.1_example_dreams.png"/>
</nobr>'

I've been using my web of named lists (each with an area to copy/paste or write text) http://github.com/benrayfield/listweb for a few years. The version there is stable enough but I'm redesigning the one I'm using cuz it got so big and I added a datetime column on the left to every item (when to look at it next, then usually click once to add a month week day hour 5minutes or combinations), usually drag and drop a bunch of them in about 30 seconds. The 2 sides up and down view the same huge list of all these notes, filenames, urls, lots of stuff I organize here. I have about 45000 names (empty lists) and a few thousand of them are lists, and I plan to use it, with various features added as needed, without starting over in an empty one ever, for the rest of my life I'm estimating I'll have about a million names in it. I'm upgrading the search.

Your data is yours as 2 files per name: an ordinary json, and a .jsonperline auto versions every 15 minutes that changes exist, manual recovery by copying to the .json the last or chosen line then restart (or there is a menu item thats slow in this rare action). It wont hurt the jsonperline backups even in most OS failures as its opened in append only, but everyone should backup to other devices. It has worked on a remote network drive. Creates an acyc dir right beside the executable you doubleclick or uses the one it finds there.


This is a web. Anything can connect to anything.
If x is in y's list, y is in x's list,
but they can be different orders (try by priority or time).

Left click goes into a name
Right click backs out of a name
Left and right click together selects a name
Drag any name in or drag to reorder (useful if ordered highest priority closer to middle of window)

The + button adds a name, and - removes it.

You can search by parts of words like "ord sea" finds names containing both "search" and "words"

No save button. All changes are automaticly saved every 1 minute
and copied to version history every 15 minutes, when changes exist.
To get those versions, like if you delete or change by accident,
they're the .jsonperline files in "+Root.jsonVarDir
The .json files are smaller and easier to read and are the newest of each name.

Listweb is opensource GNU GPL 2+
To get the source code, unzip this jar file which you doubleclicked.

(no relation to Listweb Comunicação & TI)
