#!/usr/bin/env python3
"""Patch the pinned Telegram Storage Usage chooser to reflect custom limits.

Native cleanup and cache_limit persistence remain upstream. For a custom 2-32
GB preference absent from the coarse chooser, insert exactly that value in
ascending order. This prevents the stock screen wrongly displaying No Limit.
Refuse to patch source if the pinned anchor has changed.
"""
import argparse
from pathlib import Path

TARGET=Path("TMessagesProj/src/main/java/org/telegram/ui/CacheControlActivity.java")
INSERT_BEFORE="                    options.add(Integer.MAX_VALUE);\n"
PATCH="""                    // ULT: keep a precise custom limit visible when this
                    // stock chooser would otherwise fall back to No Limit.
                    final int selectedCustomLimit = SharedConfig.getPreferences()
                            .getInt("cache_limit", Integer.MAX_VALUE);
                    if (selectedCustomLimit >= 2 && selectedCustomLimit <= 32
                            && !options.contains(selectedCustomLimit)) {
                        int ultCacheInsertPosition = 0;
                        while (ultCacheInsertPosition < options.size()
                                && options.get(ultCacheInsertPosition) < selectedCustomLimit) {
                            ultCacheInsertPosition++;
                        }
                        options.add(ultCacheInsertPosition, selectedCustomLimit);
                    }
"""
def patch(text):
    if "final int selectedCustomLimit" in text:
        raise ValueError("Cache limit already integrated; refusing duplicate")
    n=text.count(INSERT_BEFORE)
    if n!=1:
        raise ValueError(f"Pinned Storage Usage anchor drifted: {n} occurrences")
    return text.replace(INSERT_BEFORE,PATCH+INSERT_BEFORE,1)

def integrate(root: Path):
    path=root/TARGET
    text=path.read_text(encoding="utf-8")
    modified=patch(text)
    path.write_text(modified,encoding="utf-8")
    return path

if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("source_dir",type=Path)
    print(integrate(parser.parse_args().source_dir))
