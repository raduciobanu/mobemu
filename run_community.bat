java -cp .;mobEmu.jar;lib/* mobemu.MobEmu 0.2 0.2 0.6
java -cp .;mobEmu.jar;lib/* mobemu.MobEmu 0.2 0.4 0.4
java -cp .;mobEmu.jar;lib/* mobemu.MobEmu 0.2 0.6 0.2
java -cp .;mobEmu.jar;lib/* mobemu.MobEmu 0.4 0.2 0.4
java -cp .;mobEmu.jar;lib/* mobemu.MobEmu 0.4 0.4 0.2
java -cp .;mobEmu.jar;lib/* mobemu.MobEmu 0.6 0.2 0.2
REM FOR %%A IN (%list%) DO echo %%A 
REM FOR %%A IN (list) DO FOR %%B IN (0.25 0.5 0.75) DO java -cp .;mobEmu.jar;lib/* mobemu.MobEmu %%A %%B