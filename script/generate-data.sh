#!/bin/bash

repeating_file="data/repeating.data"
random_file="data/random.data"
random_file2="data/random2.data"
line_count=65000000
text="songsong project"

echo "Generating $line_count repeating lines in $repeating_file..."
yes "$text" | head -n "$line_count" > "$repeating_file"

echo "Generating 1GB random file in random.data"
head -c 1G </dev/urandom > "$random_file"

cp "$random_file" "$random_file2"
echo "File creation completed."
