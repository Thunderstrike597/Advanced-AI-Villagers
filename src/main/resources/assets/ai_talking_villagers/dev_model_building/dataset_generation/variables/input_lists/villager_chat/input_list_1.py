VILLAGER_1_INPUTS = [
    # --- Greetings & Farewells ---
    ("morning",                         {"Time": ["Morning"]}),
    ("good morning neighbour",          {"Time": ["Morning"]}),
    ("afternoon",                       {"Time": ["Day"]}),
    ("evening",                         {"Time": ["Dusk"]}),
    ("goodnight",                       {"Time": ["Night", "Dusk"]}),
    ("heading home already",            {"Time": ["Dusk", "Night"], "Home": ["Housed"]}),
    ("stay safe out there",             {"Loc": ["Wild", "Village"]}),  # no restriction
    ("see you tomorrow",                {}),                             # always valid

    # --- Small Talk ---
    ("busy day today",                  {"Time": ["Day", "Morning"]}),
    ("quiet day isn't it",              {"Time": ["Day", "Morning"]}),
    ("nice weather for once",           {"Weather": ["Clear"]}),
    ("awful weather today",             {"Weather": ["Rain", "Snow", "Thunder"]}),
    ("did you sleep well",              {"Time": ["Morning", "Day"]}),
    ("long day",                        {"Time": ["Dusk", "Day"]}),
    ("tired today",                     {}),

    # --- Work & Trade ---
    ("The harvest was great, So many crops!",              {"Profession": ["Farmer"]}),
    ("good trades today!",               {"Time": ["Day", "Morning"]}),
    ("any trades left?",                 {"Time": ["Day", "Dusk"]}),
    ("run out of stock already.",        {"Time": ["Day", "Dusk"]}),
    ("what are you working on?",         {}),
    ("need any help?",                   {}),
    ("how is business?",                 {"Time": ["Day", "Morning"]}),
    ("slow day for trading.",            {"Time": ["Day"], "Proffession": ["!Unemployed"]}),
    ("anything new in stock?",           {"Time": ["Day", "Morning"]}),
    ("made anything good lately?",       {}),

    # --- Village Life ---
    ("have you heard the news?",         {}),
    ("something feels off today.",       {}),
    ("did you hear that sound last night?",    {"Time": ["Morning", "Day"]}),
    ("getting harder to sleep these days..", {}),
    ("another rough night",             {"Time": ["Morning", "Day"]}),
    ("worried about tonight.",           {"Time": ["Dusk", "Day"]}),
    ("think we are safe here..",          {"Loc": ["Village"]}),
    ("how long have you lived here?",    {"Loc": ["Village"], "Home": ["Housed"]}),

    # --- Inter-villager Relations ---
    ("you doing alright?",               {}),
    ("you look tired.",                  {}),
    ("you seem down..",                   {}),
    ("cheer up!",                        {}),
    ("good to see you!",                 {}),
    ("haven't seen you in a while..",     {}),
    ("where have you been?",             {}),
    ("been meaning to talk to you.",     {}),
    ("can I ask you something?",         {}),
    ("do you know anyone who can help?", {}),

    # --- Children ---
    ("want to play?",                    {"Age": ["Child"]}),
    ("what are you doing?",              {"Age": ["Child"]}),
    ("come look at this!",               {"Age": ["Child"]}),
    ("I found something..",               {"Age": ["Child"]}),
    ("do you want to be friends?",       {"Age": ["Child"]}),
]