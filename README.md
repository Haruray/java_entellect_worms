# Read HNK's Greedy Algoritm Worm Bot

## Algoritma
Algoritma yang digunakan pada bot ini adalah greedy algoritm dengan strategi Greedy by Position untuk penentuan worm target serta Greedy by Damage untuk penentuan aksi. Worm permain akan mendekati worm musuh terdekat sampai mencapai attack range kemudian akan menyerang worm musuh.

## Environment Requirements

Java SE Development Kit 8 : http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

IntelliJ IDEA : https://www.jetbrains.com/idea/download/

Entellect game engine : https://github.com/EntelectChallenge/2019-Worms/releases/tag/2019.3.2

## Building

Setelah clone repository, buka project pada IntelliJ IDEA.
Package bot dengan cara membuka Maven Projects pada bagian kanan layar IntelliJ IDEA. Double click "java-sample-bot" > "Lifecycle" > "Install".
Ini akan membuat .jar file pada folder "target" dengan nama file "java-sample-bot-jar-with-dependencies.jar".

## Running 

Ubah path player-a pada file "game-runner-config.json" menjadi lokasi folder bot.
Buka "run.bat"

Untuk visualisasi dapat menggunakan visualizer pada link berikut
https://github.com/dlweatherhead/entelect-challenge-2019-visualiser/releases/tag/v1.0f1
Dengan cara memindahkan "match-logs" pada "starter-pack" ke folder "Matches" pada visualizer, kemudian jalankan visualizer.

## Author

### Read HNK
1. Muhammad Galih Raihan Ramadhan - 13519017
2. Safiq Faray - 13519145
3. Muhammad Iqbal Sigid - 13519152

"read hnk" -sultanh
