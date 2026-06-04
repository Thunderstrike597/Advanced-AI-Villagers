CASUAL_INPUTS = [
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
]

# Player follow-up inputs for turns 2 and 3 of a conversation.
# These are deliberately open-ended so the villager's reply
# is driven by context + conversation history, not a new topic.
FOLLOWUP_INPUTS = [
    "I see", "really?", "are you sure?", "okay",
    "that's rough", "what do you mean?", "go on",
    "why?", "what happens now?", "what should I do?",
    "can I help?", "stay safe", "I understand",
]
