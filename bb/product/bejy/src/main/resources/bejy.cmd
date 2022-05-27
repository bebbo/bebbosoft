@for /f " tokens=* delims=;" %%b in ('type dospath.txt') do @set BEJYCP= %%b
java -cp %BEJYCP%;servlets  de.bb.bejy.Main
