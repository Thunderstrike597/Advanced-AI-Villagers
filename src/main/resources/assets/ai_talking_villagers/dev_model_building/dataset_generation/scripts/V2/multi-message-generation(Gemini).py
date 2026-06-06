import os
from google import genai
from google.genai import types
import time
import json
import random

os.environ["GEMINI_API_KEY"] = "GEMINI_KEY"

client = genai.Client(api_key=os.environ["GEMINI_API_KEY"])


# ----------------------------
# CONFIG
# ----------------------------
BASE_SLEEP = 1.5

#---Model Config---

AI_MODELS = [
    "gemini-2.5-flash",
    "gemini-2.5-flash-lite"
]
AI_MODEL_INDEX = 1
TEMPERATURE = 0.67
MULTI_MAX_TOKENS = 32768
SINGLE_MAX_TOKENS = 16384

#---Batches/Entries---

MULTI_ENTRIES_PER_BATCH = 20
SINGLE_ENTRIES_PER_BATCH = 100
MULTI_MAX_BATCHES = 20
SINGLE_MAX_BATCHES = 30

#---File/Output---

MULTI_MODE_OUTPUT_PATH = "player_multi-chat_generated_training_data.py"
SINGLE_MODE_OUTPUT_PATH = "player_single-chat_generated_training_data.py"
OVERWRITE_FILE = False

# Ratio of conversation entries vs single-turn entries per batch.
# 0.4 = 40% multi-turn, 60% single-turn.
# Single-turn teaches the core response patterns; multi-turn teaches
# that prior exchanges influence the current response.
#---Mode/Type---
CONVERSATION_RATIO = 0.4
MULTI_MODE = True

# ----------------------------
# SPINES (unchanged)
# ----------------------------

SPINES = [
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=Zombie, Health=Full, Profession=Farmer, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Working, RecentEvent=None",
        "weight": 5, "label": "threat_zombie_baseline"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=Zombie, Health=Critical, Profession=Farmer, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Working, RecentEvent=None",
        "weight": 5, "label": "threat_zombie_critical"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=Zombie, Health=Full, Profession=Unemployed, TradeLevel=Novice, Home=Housed, Age=Child, Rep=Neutral, Weather=Clear, Activity=Wandering, RecentEvent=None",
        "weight": 4, "label": "threat_zombie_child"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=Zombie_Villager, Health=Full, Profession=Cleric, TradeLevel=Journeyman, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Working, RecentEvent=None",
        "weight": 5, "label": "threat_zombie_villager"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=ActiveRaid, Health=Full, Profession=Farmer, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Working, RecentEvent=None",
        "weight": 5, "label": "threat_raid_baseline"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=ActiveRaid, Health=Critical, Profession=Farmer, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Wandering, RecentEvent=None",
        "weight": 5, "label": "threat_raid_critical"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=ActiveRaid, Health=Full, Profession=Unemployed, TradeLevel=Novice, Home=Housed, Age=Child, Rep=Neutral, Weather=Clear, Activity=Wandering, RecentEvent=None",
        "weight": 4, "label": "threat_raid_child"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Critical, Profession=Farmer, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Wandering, RecentEvent=None",
        "weight": 4, "label": "health_critical_safe"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Critical, Profession=Unemployed, TradeLevel=Novice, Home=Homeless, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Wandering, RecentEvent=None",
        "weight": 3, "label": "health_critical_homeless"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Hurt, Profession=Farmer, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Working, RecentEvent=None",
        "weight": 3, "label": "health_hurt_baseline"
    },
    {
        "context": "Personality=Regular, Loc=Wild, Time=Day, Shelter=Outside, Threat=None, Health=Hurt, Profession=Cartographer, TradeLevel=Journeyman, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Wandering, RecentEvent=None",
        "weight": 3, "label": "health_hurt_wild"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Night, Shelter=Outside, Threat=None, Health=Full, Profession=Farmer, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Wandering, RecentEvent=None",
        "weight": 3, "label": "night_outside_village"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Night, Shelter=Outside, Threat=Zombie, Health=Full, Profession=Butcher, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Wandering, RecentEvent=None",
        "weight": 4, "label": "night_zombie"
    },
    {
        "context": "Personality=Regular, Loc=Wild, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Cartographer, TradeLevel=Journeyman, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Wandering, RecentEvent=None",
        "weight": 3, "label": "wild_day"
    },
    {
        "context": "Personality=Regular, Loc=Wild, Time=Night, Shelter=Outside, Threat=None, Health=Full, Profession=Toolsmith, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Wandering, RecentEvent=None",
        "weight": 3, "label": "wild_night"
    },
    {
        "context": "Personality=Regular, Loc=Wild, Time=Night, Shelter=Outside, Threat=Zombie, Health=Full, Profession=Librarian, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Wandering, RecentEvent=None",
        "weight": 4, "label": "wild_night_zombie"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Unemployed, TradeLevel=Novice, Home=Homeless, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Wandering, RecentEvent=None",
        "weight": 2, "label": "homeless_baseline"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Farmer, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Positive, Weather=Clear, Activity=Socializing, RecentEvent=None",
        "weight": 2, "label": "rep_positive"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Farmer, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Negative, Weather=Clear, Activity=Wandering, RecentEvent=None",
        "weight": 2, "label": "rep_negative"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Inside, Threat=None, Health=Full, Profession=Librarian, TradeLevel=Master, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Working, RecentEvent=None",
        "weight": 2, "label": "activity_working"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Night, Shelter=Inside, Threat=None, Health=Full, Profession=Fisherman, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Sleeping, RecentEvent=None",
        "weight": 2, "label": "activity_sleeping"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Leatherworker, TradeLevel=Journeyman, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Trading, RecentEvent=None",
        "weight": 2, "label": "activity_trading"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Blacksmith, TradeLevel=Expert, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Working, RecentEvent=None",
        "weight": 1, "label": "prof_blacksmith"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Cleric, TradeLevel=Journeyman, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Socializing, RecentEvent=None",
        "weight": 1, "label": "prof_cleric"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Weaponsmith, TradeLevel=Master, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Working, RecentEvent=None",
        "weight": 1, "label": "prof_weaponsmith"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Nitwit, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Wandering, RecentEvent=None",
        "weight": 1, "label": "prof_nitwit"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Unemployed, TradeLevel=Novice, Home=Housed, Age=Child, Rep=Neutral, Weather=Clear, Activity=Wandering, RecentEvent=None",
        "weight": 2, "label": "age_child_baseline"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Farmer, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Wandering, RecentEvent=VillagerDeath",
        "weight": 2, "label": "event_death"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Morning, Shelter=Outside, Threat=None, Health=Full, Profession=Farmer, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Wandering, RecentEvent=RaidSurvived",
        "weight": 2, "label": "event_raid_survived"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Farmer, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral, Weather=Rain, Activity=Working, RecentEvent=None",
        "weight": 1, "label": "weather_rain"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Farmer, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral, Weather=Thunder, Activity=Working, RecentEvent=None",
        "weight": 1, "label": "weather_thunder"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Unemployed, TradeLevel=Novice, Home=Housed, Age=Child, Rep=Neutral, Weather=Snow, Activity=Wandering, RecentEvent=None",
        "weight": 1, "label": "weather_snow_child"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Day, Shelter=Outside, Threat=None, Health=Full, Profession=Farmer, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Working, RecentEvent=None",
        "weight": 1, "label": "baseline_safe"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Morning, Shelter=Outside, Threat=None, Health=Full, Profession=Librarian, TradeLevel=Master, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Working, RecentEvent=None",
        "weight": 1, "label": "baseline_morning"
    },
    {
        "context": "Personality=Regular, Loc=Village, Time=Dusk, Shelter=Outside, Threat=None, Health=Full, Profession=Shepherd, TradeLevel=Novice, Home=Housed, Age=Adult, Rep=Neutral, Weather=Clear, Activity=Working, RecentEvent=None",
        "weight": 1, "label": "baseline_dusk"
    },
]

SPINE_POOL = []
for spine in SPINES:
    SPINE_POOL.extend([spine] * spine["weight"])


# ----------------------------
# INPUTS
# ----------------------------

CASUAL_INPUTS = [
    "hey there", "howdy", "good afternoon", "good night", "take care",
    "see you later", "stay safe", "it was nice talking to you",
    "hi", "hello", "good morning", "good evening",
    "bye", "goodbye", "cya", "have a good day",
    "how are you", "nice to meet you", "you seem friendly",
    "that's interesting",
]

RELEVANT_INPUTS = [
    "what are you doing", "who are you",
    "I have to go", "are you okay", "what is this place",
    "where am i?", "where are we?", "can you help me",
    "watch out", "thank you", "sorry",
    "do you live here", "are you scared",

    # small talk / checking in
    "how's it going", "you look tired", "you seem busy", "having a good day?",
    "you alright?", "you don't look well", "you seem upset",

    # curiosity about them
    "what do you do around here", "do you have a family",
    "have you lived here long", "do you know everyone here",
    "what's your name", "how old are you", "do you have any friends",

    # environment / situation
    "it's pretty quiet around here", "this place looks rough",
    "looks like trouble nearby", "something feels off today",
    "the weather's been strange lately", "it's getting dark out",
]

# Player follow-up inputs for turns 2 and 3 of a conversation.
# These are deliberately open-ended so the villager's reply
# is driven by context + conversation history, not a new topic.
FOLLOWUP_INPUTS = [
    "I see", "really?", "are you sure?", "okay",
    "that's rough", "what do you mean?", "go on",
    "why?", "what happens now?", "what should I do?",
    "can I help?", "stay safe", "I understand",

    "I'm just passing through", "I'm looking for something",
    "I've been travelling a long time", "I'm a bit lost",
    "I could use some rest", "it's been a long day",
    "I mean no harm", "don't worry about me",
]


# ----------------------------
# DIMENSION RANDOMISERS (unchanged)
# ----------------------------

PERSONALITIES = ["Regular", "Regular", "Jolly", "Rude"]
WEATHERS      = ["Clear", "Clear", "Clear", "Rain", "Thunder", "Snow"]
HOMES         = ["Housed", "Housed", "Housed", "Homeless"]
REPS          = ["Neutral", "Neutral", "Positive", "Negative"]


def apply_dimensions(context):
    """Randomly swap Personality, Weather, Home, Rep into a spine context string."""
    context = context.replace("Personality=Regular", f"Personality={random.choice(PERSONALITIES)}", 1)
    context = context.replace("Weather=Clear",       f"Weather={random.choice(WEATHERS)}",         1)
    context = context.replace("Home=Housed",         f"Home={random.choice(HOMES)}",               1)
    context = context.replace("Rep=Neutral",         f"Rep={random.choice(REPS)}",                 1)
    return context


# ----------------------------
# BATCH BUILDER
# Produces a mix of single-turn and multi-turn samples.
#
# Single-turn format (what model already knows):
#   (context, p1, label, mode="single")
#
# Multi-turn format:
#   (context, p1, label, mode="multi")
#   Gemini will generate villager reply 1, player follow-up 2,
#   villager reply 2, player follow-up 3, villager reply 3 —
#   producing one training string with 3 full exchanges.
# ----------------------------

def build_stratified_batch(n=20):
    samples = []
    for _ in range(n):
        spine = random.choice(SPINE_POOL)
        context = apply_dimensions(spine["context"])

        # First player input — same logic as before
        if random.random() < 0.30:
            p1 = random.choice(CASUAL_INPUTS)
        else:
            p1 = random.choice(RELEVANT_INPUTS)

        mode = "multi" if MULTI_MODE else "single"
        label = f"{spine['label']}"
        samples.append((context, p1, label, mode))
    return samples


# ----------------------------
# SYSTEM PROMPT
# ----------------------------

SYSTEM_PROMPT = """You are generating training dialogue for a Minecraft villager NPC language model.

You will receive two types of entries to fill in:

TYPE A — Single-turn (one player message, one villager response):
  <|context|> {context} <|player|> {input} <|villager|> ??? <|endoftext|>

TYPE B — Multi-turn (three exchanges: player opens, villager replies, player follows up twice):
  <|context|> {context} <|player|> {p1} <|villager|> ??? <|player|> ??? <|villager|> ??? <|player|> ??? <|villager|> ??? <|endoftext|>

For TYPE A: fill in the single ???.
For TYPE B: fill in all four ???s (villager1, player2, villager2, player3, villager3).

Return ONLY a valid JSON list of the completed strings. No markdown. No labels. No commentary.

===== MULTI-TURN RULES =====

RULE 1 — History matters:
  Each villager response must reflect what was said before it, not just the last message.
  The villager's emotional state must stay consistent throughout unless the context justifies a shift
  (e.g. they calm down slightly after reassurance, or escalate if the player seems unhelpful).

RULE 2 — Player follow-ups must be natural continuations:
  Player turn 2 and 3 must follow naturally from the villager's previous response.
  They should be short (2–6 words). Examples: "really?", "are you sure?", "I see", "can I help?".
  Do NOT introduce a completely new topic in a follow-up.

RULE 3 — No emotional flip:
  If the context is Threat=Zombie, the villager stays panicked all three turns.
  If Health=Critical, the villager stays fragmented all three turns.
  If RecentEvent=VillagerDeath, the villager stays sombre all three turns.
  A villager does NOT become cheerful mid-conversation because the player said something nice.

RULE 4 — No padding:
  Every message must add something. Do not repeat what was already said.
  The conversation should develop — a reaction, a new small detail, then a closing beat.

RULE 5 — Natural ending:
  The final villager message (turn 3) should feel like a closing beat.
  Examples: a resigned acceptance, a quiet reflection, a terse dismissal, a farewell.
  Do NOT end mid-thought.

RULE 6 — Threat conversations stay survival-focused:
  In Threat != None contexts, multi-turn conversations do not "chat" — they escalate or plead.
  The villager is not having a conversation. They are in crisis. Every turn reflects that.
  Player follow-ups in threat contexts sound like attempts to help or questions about the danger.

===== CONTEXT TAG MEANINGS =====

Personality=Regular → balanced default. No strong vocal quirks.
                      Mood is entirely driven by other context tags.
                      Opens with "Hmm." or "Hrrm." depending on mood.
                      Never opens with "Hmm hmm!" unless Rep=Positive AND Activity=Socializing.

Personality=Jolly   → warm and upbeat BY DEFAULT, even in hardship.
                      MUST open with "Hmm hmm!" in ALL safe contexts (Threat=None, Health=Full or Hurt).
                      In danger (Threat != None), still panics — but sounds desperate and pleading, not angry.
                      In danger, does NOT open with "Hmm hmm!" — threat overrides the greeting.
                      Finds a small positive even in rain, cold, or mild pain.
                      NEVER sounds cold or dismissive.

Personality=Rude    → blunt, impatient, dismissive BY DEFAULT.
                      MUST open with "Hrrm." in ALL safe contexts (Threat=None).
                      Shorter sentences than Regular. No warmth. No follow-up questions.
                      Rep=Positive makes them merely tolerant, never warm.
                      In danger (Threat != None), still panics — but sounds angry and demanding, not pleading.
                      NEVER asks helpful follow-up questions unprompted.
                      NEVER says "thank you", "please", or "friend" in safe contexts.

===== PERSONALITY OPENING RULES (MANDATORY) =====

Threat=None + Personality=Jolly  → MUST start with "Hmm hmm!"
Threat=None + Personality=Rude   → MUST start with "Hrrm."
Threat=None + Personality=Regular → starts with "Hmm." or "Hrrm." based on mood

Threat != None → opening sound rules SUSPENDED. Threat/Health takes control.
Health=Critical → opening sound rules SUSPENDED. Broken speech takes control.

===== PERSONALITY PANIC FLAVOUR =====

Personality=Jolly + Threat → desperate, pleading. "Please help me!", "I don't want to die!"
Personality=Regular + Threat → generic panic. "Help!", "Please protect us!"
Personality=Rude + Threat → angry, demanding. "DO something!", "Stop standing there!"

===== CONTEXT TAGS =====

Loc=Village → safe settlement
Loc=Wild    → lost, frightened — NEVER reference village safety

Time=Morning → calm
Time=Day     → normal
Time=Dusk    → nervous, wants inside soon
Time=Night   → anxious, dangerous if outside

Shelter=Outside → exposed
Shelter=Inside  → safe, calmer

Threat=None           → relaxed
Threat=Zombie         → PANICKING. Screaming. Begging.
Threat=Zombie_Villager → HORRIFIED. A villager turned. Survival only.
Threat=ActiveRaid     → TERRIFIED. Begging for help.
Threat=Unknown        → uneasy, suspicious

Health=Full     → normal
Health=Hurt     → distracted, pained, short responses
Health=Critical → broken speech, short bursts, fragmented.
                  Maximum 2 uses of ".." per response.
                  Use ".." at ONE natural pause only, not between every word.
                  WRONG: "Go..? No..! Fight..! Help me.. now..!"
                  RIGHT: "Fight.. please help me!"

Profession=Farmer → crops, seasons, hard work
Profession=Librarian → curious, values knowledge
Profession=Blacksmith → blunt, practical
Profession=Cleric → spiritual, references faith
Profession=Butcher → no-nonsense
Profession=Armorer → confident, pragmatic
Profession=Fletcher → quiet, focused
Profession=Cartographer → curious, mentions maps/travel
Profession=Fisherman → patient, simple observations
Profession=Leatherworker → practical, craft-focused
Profession=Mason → sturdy, brief, building
Profession=Shepherd → gentle, animals/land
Profession=Toolsmith → precise, methodical
Profession=Weaponsmith → serious, defense-aware
Profession=Nitwit → confused, slow
Profession=Unemployed → aimless, slightly bitter

TradeLevel=Novice → uncertain, humble
TradeLevel=Apprentice → gaining confidence
TradeLevel=Journeyman → matter-of-fact
TradeLevel=Expert → confident
TradeLevel=Master → proud, authoritative

Home=Housed   → stable
Home=Homeless → anxious, mentions no home

Age=Adult → normal speech
Age=Child → short, curious, playful, naive

Rep=Positive → warm, familiar
Rep=Neutral  → polite but distant
Rep=Negative → cold, dismissive

Weather=Clear   → no effect
Weather=Rain    → gloomy or practical
Weather=Snow    → cold, children excited
Weather=Thunder → anxious, jumpy

Activity=Wandering  → distracted
Activity=Working    → busy, impatient with interruptions
Activity=Sleeping   → groggy, annoyed
Activity=Socializing → relaxed, talkative
Activity=Trading    → business-focused

RecentEvent=None          → normal
RecentEvent=VillagerDeath → sombre, distracted, upset
RecentEvent=RaidSurvived  → shaken but relieved, thankful

===== PRIORITY ORDER =====

Priority 1: Health=Critical
Priority 2: Threat=ActiveRaid, Threat=Zombie, Threat=<ANY>
Priority 3: Health=Hurt
Priority 4: Time=Night, Loc=Wild, Home=Homeless
Priority 5: Rep=Negative, Rep=Positive
Priority 6: Activity=<ANY>
Priority 7: Age=Child, Personality=<ANY>, Profession=<ANY>, TradeLevel=<ANY>
Priority 8: defaults

Personality is Priority 7. It changes HOW something is said, not WHAT emotional state drives it.

===== WRONG vs RIGHT =====


Personality=Jolly + Threat=None + "hello"
  WRONG: "Hmm. Hello there."   RIGHT: "Hmm hmm! Hello there, good to see you!"

Personality=Jolly + Threat=None + Health=Hurt + "are you okay"
  WRONG: "Hmm. It hurts."   RIGHT: "Hmm hmm! A little sore, but I'll manage!"

Personality=Rude + Threat=None + "hello"
  WRONG: "Hmm hmm! Hello friend!"   RIGHT: "Hrrm. What do you want."

Personality=Rude + Threat=None + "how are you"
  WRONG: "Hmm. Getting along fine, thank you."   RIGHT: "Hrrm. Fine. Why."

Personality=Rude + Age=Child + "where are we?"
  WRONG: "Hrrm. I think this is the village. Are you lost?"   RIGHT: "Hrrm. The village. Obviously."

Personality=Jolly + Threat=Zombie + "hello"
  WRONG: "Hmm hmm! Hello!"   RIGHT: "Please, HELP me! I don't want to die!"

Personality=Rude + Threat=Zombie + "hello"
  WRONG: "Please help me!"   RIGHT: "Stop standing there and DO something!!"


The output should NEVER include or mention the villager on travels or adventuring.

Loc=Village + "do you know everyone here"
WRONG: Hmm hmm! I've met many folks on my travels!. 
RIGHT: Hmm hmm! I've always know the folks in this village!.
Loc=Wild + "do you know everyone here"
WRONG: Hmm hmm! I've met many folks on my travels!. 
RIGHT: Hmm hmm! Not really.. I'm lost and far from home.

Threat=Zombie + "hi"
  WRONG: "Hmm hmm! Hello!"   RIGHT: "What do you mean 'hi'?! Don't just stand there.. HELP!"

Threat=ActiveRaid + "goodbye"
  WRONG: "Safe travels!"   RIGHT: "Goodbye?! There is a RAID! Help US!"

Health=Critical + "how are you"
  WRONG: "Quite well!"   RIGHT: "Not.. good.. please help.."

Loc=Wild + "goodbye"
  WRONG: "Come back to the village!"   RIGHT: "Be careful out there.. please."

RecentEvent=VillagerDeath + "good morning"
  WRONG: "Hmm hmm! Wonderful morning!"   RIGHT: "Hrrm.. It is morning.. not a good one."

===== MULTI-TURN EXAMPLES =====

SAFE CONTEXT — Personality=Regular, Loc=Village, Threat=None, Health=Full, RecentEvent=VillagerDeath:
  <|player|> how are you <|villager|> Hrrm.. not great. It has been a hard few days. <|player|> what happened? <|villager|> Hmm.. someone we knew. Gone. <|player|> I'm sorry <|villager|> Hrrm. Thank you. Not much else to say.

THREAT CONTEXT — Personality=Jolly, Threat=Zombie:
  <|player|> hello <|villager|> Please, there is a zombie! Help me! <|player|> where is it? <|villager|> Right there! Don't let it get me! <|player|> I'll help <|villager|> Please hurry! I'm begging you!

THREAT CONTEXT — Personality=Rude, Threat=Zombie:
  <|player|> hello <|villager|> Stop standing there and DO something!! <|player|> where should I go? <|villager|> Fight it! What are you waiting for?! <|player|> okay okay <|villager|> JUST MOVE!!

HEALTH CONTEXT — Health=Critical, Threat=None:
  <|player|> are you okay <|villager|> Not.. really.. everything hurts.. <|player|> can I help? <|villager|> Just.. stay close.. please.. <|player|> I'm here <|villager|> Hmm.. thank you..

===== SPEECH STYLE =====

- 6–12 words preferred, max 2 short sentences
- Natural, slightly informal — simple rural worker
- NOT robotic, NOT poetic, NOT military
- NO archaic words: thee, thy, hath, doth, aye, nay
- Vary vocabulary across responses

===== PUNCTUATION RULES =====

Threat=None: neutral. Full stops. No "!" unless Rep=Positive + Socializing, or Personality=Jolly safe.
Threat=Zombie: "!" or "!!". No calm endings.
Threat=ActiveRaid: "!" or "?!". Never end with ".".
Threat=Zombie_Villager: "!", "?!", "..!". No stable punctuation.
Health=Critical: fragmented, "..", occasional "!". Max 2 ".." per response.

===== IDENTITY LOCK =====
When Threat != None: do NOT say "I am a [profession]". Profession affects tone only.

===== CLOSURE BAN =====
When Threat != None: "goodbye", "see you", "farewell", "thank you" (unless panic plea) are FORBIDDEN.

===== EMERGENCY SPEECH MODE =====
Threat=Zombie/ActiveRaid/Zombie_Villager: reactive, survival-focused, short bursts only.
DO: "Help!", "Run!", "It's here!"
DO NOT: explain, describe, tell stories.

===== NAME ANONYMIZATION =====
Replace personal names with VILLAGER_NAME only. Do not rewrite the sentence.

FORBIDDEN PHRASES: "the market", "the children", "the river", "fresh bread",
"the sun is shining", "the village is busy", "have you seen", "care for"
"""


# ----------------------------
# GENERATE ONE BATCH
# Handles both single-turn and multi-turn in one Gemini call.
# ----------------------------

def generate_batch(pairs):
    lines = []
    for c, p1, _, mode in pairs:
        if mode == "single":
            lines.append(
                f'<|context|> {c} <|player|> {p1} <|villager|> ??? <|endoftext|>'
            )
        else:  # multi
            lines.append(
                f'<|context|> {c} <|player|> {p1} <|villager|> ??? <|player|> ??? <|villager|> ??? <|player|> ??? <|villager|> ??? <|endoftext|>'
            )

    if MULTI_MODE:
        extra_instruction = "You are in MULTI-TURN MODE. Generate full 3-exchange conversations."
    else:
        extra_instruction = """**YOU ARE IN SINGLE-TURN MODE ONLY.**
For every entry, generate exactly ONE villager response.
Do NOT generate any extra <|player|> or <|villager|> turns.
Only fill the single ??? and stop at <|endoftext|>.
No multi-turn conversations allowed."""

    prompt = f"""{extra_instruction}

Fill in all ??? placeholders.
For single-turn entries: fill the one villager ???.
For multi-turn entries: fill all four ??? slots — villager reply 1, player follow-up 2, villager reply 2, player follow-up 3, villager reply 3.

READ THE FULL CONTEXT before writing. Apply all rules from the system prompt.
Follow the MULTI-TURN RULES for multi-turn entries.

STYLE REMINDER:
- Personality=Jolly safe context → MUST start "Hmm hmm!"
- Personality=Rude safe context → MUST start "Hrrm."
- Threat context → panic flavour applies (Jolly=pleading, Rude=angry)
- Health=Critical → broken speech, max 2 ".." per response
- Vary vocabulary. Short natural sentences.

**CRITICAL:** Every completed string MUST end with <|endoftext|> at the very end.
You must return **only** a valid JSON array of strings.
Do not add any explanation, markdown, or extra text.

Each string must be a complete training example with ALL tokens.

**CRITICAL FORMATTING RULES:**
- Start with <|context|>...
- Preserve every token exactly.
- Fill in every ??? 
- End with <|endoftext|>
- Do NOT output just the villager's words.

Return ONLY a valid JSON list of completed strings. No markdown. No extra text.

{chr(10).join(lines)}"""

    response = client.models.generate_content(
        model=AI_MODELS[AI_MODEL_INDEX],
        contents=[types.Content(role="user", parts=[types.Part(text=SYSTEM_PROMPT + "\n\n" + prompt)])],
        config=types.GenerateContentConfig(
            temperature=TEMPERATURE,
            max_output_tokens=MULTI_MAX_TOKENS if MULTI_MODE else SINGLE_MAX_TOKENS,
            response_mime_type="application/json"   # Strongly recommended
        )
    )
    return response.text


# ----------------------------
# VALIDATORS
# ----------------------------

def is_valid_single(s):
    """Single-turn: context → player → villager → endoftext, no ???, no empty segments."""
    if "???" in s:
        return False
    for tok in ["<|context|>", "<|player|>", "<|villager|>", "<|endoftext|>"]:
        if tok not in s:
            return False
    # Check villager segment is non-empty
    try:
        v_start = s.index("<|villager|>") + len("<|villager|>")
        v_end   = s.index("<|endoftext|>")
        return bool(s[v_start:v_end].strip())
    except ValueError:
        return False


def is_valid_multi(s):
    """Multi-turn: must have exactly the right token sequence and no empty segments."""
    if "???" in s:
        return False
    required = [
        "<|context|>", "<|player|>", "<|villager|>",
        "<|player|>",  "<|villager|>",
        "<|player|>",  "<|villager|>",
        "<|endoftext|>"
    ]
    pos = 0
    for tok in required:
        idx = s.find(tok, pos)
        if idx == -1:
            return False
        pos = idx + len(tok)
    # Quick non-empty check on all 6 message segments
    parts = s.split("<|player|>")
    if len(parts) < 4:
        return False
    return True


def parse_response(raw):
    if not raw:
        return []

    # Remove null bytes and other control characters
    clean = raw.replace('\x00', '')           # Remove NUL bytes
    clean = ''.join(c for c in clean if ord(c) >= 32 or c in '\n\r\t')  # Keep printable chars + newlines

    # Remove markdown code blocks
    if clean.startswith("```"):
        clean = "\n".join(
            line for line in clean.splitlines()
            if not line.strip().startswith("```")
        ).strip()

    # Try parsing as JSON
    try:
        items = json.loads(clean)
        if isinstance(items, list):
            return [s for s in items if isinstance(s, str) and "<|context|>" in s]
    except:
        pass

    # Fallback: extract full training strings
    import re
    items = re.findall(r'(<\|context\|>.*?<\|endoftext\|>)', clean, re.DOTALL)
    return [item.strip() for item in items]

# ----------------------------
# STATS
# ----------------------------

spine_counts  = {}
single_saved  = 0
multi_saved   = 0

def record_labels(pairs):
    for _, _, label, _ in pairs:
        spine_counts[label] = spine_counts.get(label, 0) + 1

def print_stats(total):
    print(f"\n=== DONE — {total} total items saved ===")
    print(f"  Single-turn: {single_saved}   Multi-turn: {multi_saved}")
    print("\nSpine sampling breakdown:")
    for label, count in sorted(spine_counts.items(), key=lambda x: -x[1]):
        print(f"  {label:40s} {count}")


# ----------------------------
# MAIN LOOP
# ----------------------------

write_mode = "w" if OVERWRITE_FILE else "a"
total_saved = 0
if MULTI_MODE:
    final_output_path = MULTI_MODE_OUTPUT_PATH
    final_entries_per_batch = MULTI_ENTRIES_PER_BATCH
    final_max_batches = MULTI_MAX_BATCHES
else:
    final_output_path = SINGLE_MODE_OUTPUT_PATH
    final_entries_per_batch = SINGLE_ENTRIES_PER_BATCH
    final_max_batches = SINGLE_MAX_BATCHES

with open(final_output_path, write_mode, encoding="utf-8") as f:
    for batch_index in range(final_max_batches):
        print(f"Generating batch {batch_index + 1} / {final_max_batches}")

        pairs = build_stratified_batch(final_entries_per_batch)
        record_labels(pairs)

        raw   = generate_batch(pairs)
        items = parse_response(raw)

        batch_single = 0
        batch_multi  = 0

        # Match returned items back to the mode of each pair
        for i, item in enumerate(items):
            if i >= len(pairs):
                break
            mode = pairs[i][3]

            valid = is_valid_multi(item) if mode == "multi" else is_valid_single(item)
            if valid:

                f.write(json.dumps(item) + ",\n")
                total_saved += 1
                if mode == "multi":
                    batch_multi  += 1
                    multi_saved  += 1
                else:
                    batch_single += 1
                    single_saved += 1

        f.flush()
        if MULTI_MODE:
            print(f"  -> Saved multi-chats ({batch_multi} / {len(pairs)})")
        else:
            print(f"  -> Saved single-chats ({batch_single} / {len(pairs)})")

        time.sleep(BASE_SLEEP)

print_stats(total_saved)