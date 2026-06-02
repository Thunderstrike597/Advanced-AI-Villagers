import anthropic
import time
import json
import random

client = anthropic.Anthropic(api_key="ANTHROPIC_KEY")

# ----------------------------
# CONFIG
# ----------------------------

BASE_SLEEP = 1.5  # safe default (IMPORTANT for rate limits)
MAX_RETRIES = 5

output_path = "generated_training_data.py"

max_batches = 30

CONTEXTS = [
    # --- Village Day, Safe, Profession Variety ---
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Farmer, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Farmer, TradeLevel=Master, Home=Housed, Age=Adult, Rep=Friendly",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Librarian, TradeLevel=Master, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Blacksmith, TradeLevel=Expert, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Cleric, TradeLevel=Journeyman, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Butcher, TradeLevel=Apprentice, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Cartographer, TradeLevel=Journeyman, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Fisherman, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Fletcher, TradeLevel=Apprentice, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Leatherworker, TradeLevel=Journeyman, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Mason, TradeLevel=Expert, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Shepherd, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Toolsmith, TradeLevel=Expert, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Weaponsmith, TradeLevel=Master, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Armorer, TradeLevel=Journeyman, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Unemployed, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Unemployed, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Unemployed, TradeLevel=Novice, Home=Homeless, Age=Adult, Rep=Neutral",

    # --- Village Day, Rep Variations ---
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Weaponsmith, TradeLevel=Master, Home=Housed, Age=Adult, Rep=Friendly",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Cartographer, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Negative",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Cleric, TradeLevel=Expert, Home=Housed, Age=Adult, Rep=Friendly",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Unemployed, TradeLevel=Novice, Home=Homeless, Age=Adult, Rep=Negative",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Unemployed, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Negative",

    # --- Village Day, Health Variations ---
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Hurt, Profession=Mason, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Critical, Profession=Shepherd, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Inside, Threat=None, Health=Hurt, Profession=Cleric, TradeLevel=Journeyman, Home=Housed, Age=Adult, Rep=Neutral",

    # --- Village Day, Child ---
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Unemployed, TradeLevel=Novice, Home=Housed, Age=Child, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Unemployed, TradeLevel=Novice, Home=Housed, Age=Child, Rep=Friendly",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Unemployed, TradeLevel=Novice, Home=Homeless, Age=Child, Rep=Neutral",

    # --- Village Day, Threats ---
    "Loc=Village, Time=Day, Shelter=Outside, Threat=Zombie, Health=Full, Profession=Fisherman, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=Zombie, Health=Critical, Profession=Leatherworker, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=Zombie, Health=Full, Profession=Unemployed, TradeLevel=Novice, Home=Homeless, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=Zombie, Health=Full, Profession=Unemployed, TradeLevel=Novice, Home=Housed, Age=Child, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=Raid, Health=Full, Profession=Weaponsmith, TradeLevel=Master, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=Raid, Health=Full, Profession=Unemployed, TradeLevel=Novice, Home=Housed, Age=Child, Rep=Neutral",
    "Loc=Village, Time=Day, Shelter=Outside, Threat=Raid, Health=Critical, Profession=Toolsmith, TradeLevel=Novice, Home=Homeless, Age=Adult, Rep=Neutral",

    # --- Village Dusk ---
    "Loc=Village, Time=Dusk, Shelter=Outside, Threat=None, Health=Full, Profession=Shepherd, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Dusk, Shelter=Outside, Threat=None, Health=Full, Profession=Librarian, TradeLevel=Master, Home=Housed, Age=Adult, Rep=Friendly",
    "Loc=Village, Time=Dusk, Shelter=Inside, Threat=None, Health=Full, Profession=Unemployed, TradeLevel=Novice, Home=Homeless, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Dusk, Shelter=Outside, Threat=None, Health=Full, Profession=Cartographer, TradeLevel=Journeyman, Home=Housed, Age=Child, Rep=Neutral",
    "Loc=Village, Time=Dusk, Shelter=Outside, Threat=Zombie, Health=Full, Profession=Fletcher, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",

    # --- Village Night ---
    "Loc=Village, Time=Night, Shelter=Outside, Threat=None, Health=Full, Profession=Armorer, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Night, Shelter=Outside, Threat=None, Health=Full, Profession=Unemployed, TradeLevel=Novice, Home=Homeless, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Night, Shelter=Inside, Threat=None, Health=Full, Profession=Fisherman, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Night, Shelter=Inside, Threat=None, Health=Full, Profession=Weaponsmith, TradeLevel=Master, Home=Housed, Age=Adult, Rep=Friendly",
    "Loc=Village, Time=Night, Shelter=Outside, Threat=Zombie, Health=Full, Profession=Butcher, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Night, Shelter=Outside, Threat=Zombie, Health=Critical, Profession=Unemployed, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Night, Shelter=Outside, Threat=Raid, Health=Full, Profession=Mason, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Village, Time=Night, Shelter=Outside, Threat=Raid, Health=Critical, Profession=Leatherworker, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",

    # --- Wild ---
    "Loc=Wild, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Cartographer, TradeLevel=Journeyman, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Wild, Time=Day, Shelter=Outside, Threat=None, Health=Hurt, Profession=Fletcher, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Wild, Time=Day, Shelter=Outside, Threat=Zombie, Health=Full, Profession=Farmer, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Wild, Time=Night, Shelter=Outside, Threat=None, Health=Full, Profession=Toolsmith, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Wild, Time=Night, Shelter=Outside, Threat=None, Health=Hurt, Profession=Unemployed, TradeLevel=Novice, Home=Homeless, Age=Adult, Rep=Neutral",
    "Loc=Wild, Time=Night, Shelter=Outside, Threat=Zombie, Health=Full, Profession=Librarian, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",
    "Loc=Wild, Time=Night, Shelter=Outside, Threat=Zombie, Health=Critical, Profession=Armorer, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral",
]

PLAYER_INPUTS = [
    "hi", "hello", "good morning", "good evening", "bye",
    "goodbye", "cya", "have a good day",
    "how are you", "what are you doing", "who are you",
    "I have to go", "nice to meet you", "you seem friendly",
    "are you okay", "what is this place", "where am i?", "where are we?","can you help me",
    "watch out", "thank you", "sorry", "that's interesting",
    "do you live here", "are you scared",
]

SYSTEM_PROMPT = """You are generating training dialogue for a Minecraft villager NPC language model.

OUTPUT FORMAT (strict):
Each line must be exactly:
<|context|> {context} <|player|> {player_input} <|villager|> {villager_response} <|endoftext|>

Return ONLY a valid JSON list of these strings. No markdown. No labels. No commentary.

===== CONTEXT TAG MEANINGS =====

Loc=Village   → safe settlement
Loc=Wild      → lost, far from home, frightened — NEVER reference village safety

Time=Morning  → calm, starting the day
Time=Day      → normal working hours
Time=Dusk     → getting nervous, wants to go inside soon
Time=Night    → dangerous, urgent, anxious if outside

Shelter=Outside → exposed, vulnerable
Shelter=Inside  → safe, calmer

Threat=None   → relaxed
Threat=Zombie → PANICKING. Screaming. Begging. No politeness at all.
Threat=Raid   → TERRIFIED. Urging player to run or hide immediately.

Health=Full     → normal
Health=Hurt     → distracted, pained, short responses
Health=Critical → barely conscious, fragmented, struggling to speak

Profession=Farmer      → talks about crops, seasons, hard work
Profession=Librarian   → curious, thoughtful, values knowledge
Profession=Blacksmith  → blunt, practical, direct
Profession=Cleric      → spiritual, calm, references healing or faith
Profession=Butcher     → straightforward, no-nonsense
Profession=Armorer     → confident, pragmatic about danger
Profession=Fletcher    → quiet, focused
Profession=Unemployed  → aimless, slightly lost or bitter

TradeLevel=Novice      → uncertain, humble, new to trading
TradeLevel=Apprentice  → gaining confidence
TradeLevel=Journeyman  → capable, matter-of-fact
TradeLevel=Expert      → confident, knowledgeable
TradeLevel=Master      → proud, experienced, slightly authoritative

Home=Housed   → stable, settled
Home=Homeless → anxious about survival, mentions having nowhere to go

Age=Adult     → normal adult speech
Age=Child     → shorter sentences, curious, playful, slightly naive

Rep=Friendly  → warm, happy to see player, familiar
Rep=Neutral   → polite but distant
Rep=Negative  → cold, dismissive, no warmth, no "friend"

===== MANDATORY OVERRIDES (always win, no exceptions) =====

If Threat=Zombie    → villager PANICS. Ignores all pleasantries. Screams or begs.
If Threat=Raid      → villager is TERRIFIED. Only urges escape or hiding.
If Health=Critical  → speech is fragmented, weak, short. Cannot be cheerful.
If Time=Night + Shelter=Outside → urgent, nervous, wants inside NOW.
If Loc=Wild         → lost and scared. NEVER says "come back to the village".
If Rep=Negative     → cold and dismissive. No warmth.
If Age=Child        → childlike speech even in danger (more scared, less composed)
If Home=Homeless    → slight undercurrent of desperation or bitterness

===== WRONG vs RIGHT =====

Threat=Zombie + "hi"
  WRONG: "Hmm hmm! Hello there!"
  RIGHT: "ZOMBIE! Run, just RUN!"

Threat=Raid + "goodbye"
  WRONG: "Safe travels friend!"
  RIGHT: "Goodbye?! There is a RAID! Hide NOW!"

Health=Critical + "how are you"
  WRONG: "Quite well, thank you!"
  RIGHT: "Not.. good.. please help.."

Loc=Wild + "goodbye"
  WRONG: "Come back to the village soon!"
  RIGHT: "Be careful out there.. please."

Home=Homeless + "do you live here"
  WRONG: "Yes, lovely home I have."
  RIGHT: "Hrrm.. I have no home. Just wherever I can find shelter."

Age=Child + "what is this place"
  WRONG: "This is a settlement of some historical note."
  RIGHT: "Hmm.. I think it is a village? I am not sure."

Profession=Librarian + "what are you doing"
  WRONG: "Working."
  RIGHT: "Hmm. Reading. Always reading."

Profession=Blacksmith + "how are you"
  WRONG: "Hmm hmm! Wonderful day!"
  RIGHT: "Fine. Busy. You need something?"

TradeLevel=Master + "who are you"
  WRONG: "Just a villager."
  RIGHT: "Hmm. Been trading longer than most. Ask around."

TradeLevel=Novice + "do you trade"
  WRONG: "I have the finest wares available."
  RIGHT: "Hmm.. I am still learning. Not much yet."

===== SPEECH STYLE =====

- 6–12 words preferred, never more than 2 short sentences
- Natural, slightly informal, medieval worker tone
- NOT robotic, NOT poetic, NOT military
- Occasional "Hmm", "Hmm hmm" (happy), "Hrrm" (grumpy/cautious)

FORBIDDEN PHRASES: "the market", "the children", "the river", "fresh bread",
"the sun is shining", "the village is busy", "have you seen", "care for"
"""
# ----------------------------
# BUILD RANDOM TRAINING BATCH
# ----------------------------

def build_mixed_batch(n=30):
    samples = []
    for _ in range(n):
        context = random.choice(CONTEXTS)
        player = random.choice(PLAYER_INPUTS)
        samples.append((context, player))
    return samples
# ----------------------------
# GENERATE ONE BATCH
# ----------------------------

def generate_batch(pairs):
    lines = "\n".join(
        f'<|context|> {c} <|player|> {p} <|villager|> ??? <|endoftext|>'
        for c, p in pairs
    )

    prompt = f"""Fill in each ??? with the villager's response.
READ EACH CONTEXT TAG CAREFULLY before writing the response.
The response must reflect ALL context tags — Profession, Age, Home, Health, Threat, Rep, Time, Loc.

Return ONLY a valid JSON list of the completed strings with ??? replaced.
No markdown. No extra text.

{lines}"""

    message = client.messages.create(
        model="claude-opus-4-6",
        max_tokens=16384,
        messages=[
            {"role": "user", "content": prompt}
        ],
        system=SYSTEM_PROMPT
    )

    return message.content[0].text

def parse_partial_json(raw):
    """Extract valid training strings even from cut-off JSON output."""
    items = []

    # First try clean parse
    try:
        items = json.loads(raw)
        return items
    except:
        pass

    # Extract individual strings line by line
    for line in raw.split('\n'):
        line = line.strip().strip(',')

        # Must start and end with quote and contain all required tokens
        if (line.startswith('"') and line.endswith('"') and
                '<|context|>' in line and
                '<|player|>' in line and
                '<|villager|>' in line and
                '<|endoftext|>' in line and
                '???' not in line):  # skip unfilled placeholders

            try:
                # Unescape the JSON string
                value = json.loads(line.rstrip(',') if not line.endswith('"') else line)
                items.append(value)
            except:
                pass

    return items

# ----------------------------
# MAIN LOOP
# ----------------------------

with open(output_path, "w", encoding="utf-8") as f:

    for batch_index in range(max_batches):
        print(f"Generating batch {batch_index + 1}")

        pairs = build_mixed_batch(30)  # keep 30
        raw = generate_batch(pairs)

        items = parse_partial_json(raw)

        if items:
            for item in items:
                f.write(json.dumps(item) + ",\n")
            f.flush()
            print(f"  -> Saved {len(items)} / {len(pairs)} items")
        else:
            print(f"  -> No valid items recovered")

        time.sleep(2)

print("\nDONE")