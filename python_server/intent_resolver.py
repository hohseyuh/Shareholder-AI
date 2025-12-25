import re

class IntentResolver:
    def __init__(self):
        # Define keyword mappings
        self.patterns = {
            "job miner": [r"mine", r"dig", r"mining", r"ore", r"diamond"],
            "job guard": [r"guard", r"protect", r"fight", r"attack", r"watch"],
            "job none": [r"stop working", r"relax", r"none", r"no job"],
            "follow": [r"follow", r"come here", r"with me"],
            "stay": [r"stay", r"stop", r"wait"],
            "give loot": [r"loot", r"items", r"stash", r"inventory", r"drop"],
            "unequip": [r"unequip", r"give tool", r"give pickaxe"]
        }

    def resolve(self, text):
        if not text:
            return None

        # Clean the text
        text = text.lower().strip()

        # Check for matches
        for command, keywords in self.patterns.items():
            for pattern in keywords:
                if re.search(pattern, text):
                    return command
        
        # If no specific intent found, return the raw text 
        # so Minecraft's keyword logic can try to handle it.
        return text