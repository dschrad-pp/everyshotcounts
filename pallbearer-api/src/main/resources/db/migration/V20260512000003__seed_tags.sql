WITH ins_cats AS (
    INSERT INTO t_tag_category (code, name, display_order) VALUES
        ('SHOT_TYPE',       'Type of Shot',    1),
        ('START_LOCATION',  'Start Location',  2),
        ('MOVE',            'Move',            3),
        ('DRIBBLE_COUNT',   'Dribble Count',   4),
        ('SCREEN',          'Screen',          5),
        ('TRANSITION',      'Transition',      6),
        ('CUT',             'Cut',             7),
        ('FINISH_LOCATION', 'Finish Location', 8),
        ('FINISH_TYPE',     'Finish Type',     9),
        ('OTHER',           'Other',          10)
    RETURNING id, code
)
INSERT INTO t_tag (tag_category_id, code, name, display_order)
SELECT c.id, v.code, v.name, v.display_order
FROM ins_cats c
JOIN (VALUES
    -- SHOT_TYPE (3)
    ('SHOT_TYPE', '15FT',           '15ft',          1),
    ('SHOT_TYPE', '3PT',            '3pt',           2),
    ('SHOT_TYPE', 'DEPTH_SHOOTING', 'Depth Shooting',3),

    -- START_LOCATION (10) — SL_ prefix on the 4 names that also appear in FINISH_LOCATION
    ('START_LOCATION', 'SL_BASELINE_UNDER_HOOP', 'Baseline Under Hoop', 1),
    ('START_LOCATION', 'SL_L_BLOCK',             'L Block',             2),
    ('START_LOCATION', 'SL_L_CORNER',            'L Corner',            3),
    ('START_LOCATION', 'SL_L_ELBOW',             'L Elbow',             4),
    ('START_LOCATION', 'SL_L_WING',              'L Wing',              5),
    ('START_LOCATION', 'SL_R_BLOCK',             'R Block',             6),
    ('START_LOCATION', 'SL_R_CORNER',            'R Corner',            7),
    ('START_LOCATION', 'SL_R_ELBOW',             'R Elbow',             8),
    ('START_LOCATION', 'SL_R_WING',              'R Wing',              9),
    ('START_LOCATION', 'SL_TOK',                 'TOK',                10),

    -- MOVE (39 — added Fake Drop Step)
    ('MOVE', 'BACK_DOWN',            'Back Down',            1),
    ('MOVE', 'BEHIND_THE_BACK',      'Behind the Back',      2),
    ('MOVE', 'BETWEEN_THE_LEGS',     'Between the Legs',     3),
    ('MOVE', 'CROSS_STEP',           'Cross Step',           4),
    ('MOVE', 'DOUBLE_JAB',           'Double Jab',           5),
    ('MOVE', 'DROP_STEP',            'Drop Step',            6),
    ('MOVE', 'FAKE_DROP_STEP',       'Fake Drop Step',       7),
    ('MOVE', 'FRONT_CROSS',          'Front Cross',          8),
    ('MOVE', 'HESITATE',             'Hesitate',             9),
    ('MOVE', 'HIGH_RIP',             'High Rip',            10),
    ('MOVE', 'IN_AND_OUT',           'In and Out',          11),
    ('MOVE', 'INSIDE_PIVOT',         'Inside Pivot',        12),
    ('MOVE', 'JAB',                  'Jab',                 13),
    ('MOVE', 'LEAD_STEP',            'Lead Step',           14),
    ('MOVE', 'LOW_RIP',              'Low Rip',             15),
    ('MOVE', 'OUTSIDE_LEG_THROWOUT', 'Outside Leg Throwout',16),
    ('MOVE', 'PASS_FAKE',            'Pass Fake',           17),
    ('MOVE', 'POUND_DRIBBLE',        'Pound Dribble',       18),
    ('MOVE', 'PULL_UP_L',            'Pull Up L',           19),
    ('MOVE', 'PULL_UP_R',            'Pull Up R',           20),
    ('MOVE', 'PUSH_OUT',             'Push Out',            21),
    ('MOVE', 'RESHOOT',              'ReShoot',             22),
    ('MOVE', 'RETREAT_DRIBBLE',      'Retreat Dribble',     23),
    ('MOVE', 'REVERSE_PIVOT',        'Reverse Pivot',       24),
    ('MOVE', 'RIP',                  'Rip',                 25),
    ('MOVE', 'ROLLOUT',              'Rollout',             26),
    ('MOVE', 'RUN_OUT',              'Run Out',             27),
    ('MOVE', 'SHOT_FAKE',            'Shot Fake',           28),
    ('MOVE', 'SLIP_DRIBBLE',         'Slip Dribble',        29),
    ('MOVE', 'SLIDE_DRIBBLE_L',      'Slide Dribble L',     30),
    ('MOVE', 'SLIDE_DRIBBLE_R',      'Slide Dribble R',     31),
    ('MOVE', 'SQUARE_UP',            'Square Up',           32),
    ('MOVE', 'STEP_BACK_L',          'Step Back L',         33),
    ('MOVE', 'STEP_BACK_R',          'Step Back R',         34),
    ('MOVE', 'STEP_THROUGH',         'Step Through',        35),
    ('MOVE', 'SWING_STEP',           'Swing Step',          36),
    ('MOVE', 'TURNAROUND',           'Turnaround',          37),
    ('MOVE', 'UNDER_DRAG',           'Under Drag',          38),
    ('MOVE', 'WRAP_DRIBBLE',         'Wrap Dribble',        39),

    -- DRIBBLE_COUNT (3)
    ('DRIBBLE_COUNT', '1_DRIBBLE',  '1 Dribble',  1),
    ('DRIBBLE_COUNT', '2_DRIBBLE',  '2 Dribble',  2),
    ('DRIBBLE_COUNT', 'COMBO_MOVE', 'Combo Move', 3),

    -- SCREEN (9 — added Hold Off(Over))
    ('SCREEN', 'BOUNCE_OFF',       'Bounce Off',       1),
    ('SCREEN', 'CHAIR',            'Chair',            2),
    ('SCREEN', 'FLAT_BALL_SCREEN', 'Flat Ball Screen', 3),
    ('SCREEN', 'PICK_AND_ROLL',    'Pick and Roll',    4),
    ('SCREEN', 'RESCREEN',         'Rescreen',         5),
    ('SCREEN', 'SIDE_SCREEN',      'Side Screen',      6),
    ('SCREEN', 'SPLIT',            'Split',            7),
    ('SCREEN', 'TURN_DOWN',        'Turn Down',        8),
    ('SCREEN', 'HOLD_OFF_OVER',    'Hold Off(Over)',   9),

    -- TRANSITION (2)
    ('TRANSITION', 'FAST_BREAK', 'Fast Break', 1),
    ('TRANSITION', 'TRANSITION',  'Transition', 2),

    -- CUT (4)
    ('CUT', 'CURL_CUT', 'Curl Cut', 1),
    ('CUT', 'FACE_CUT', 'Face Cut', 2),
    ('CUT', 'FADE_CUT', 'Fade Cut', 3),
    ('CUT', 'RELOCATE', 'Relocate', 4),

    -- FINISH_LOCATION (15) — FL_ prefix avoids collision with SL_ codes
    -- 3pt Wing L/R renamed from Wing 3 L/R to match ultimatedata.csv
    ('FINISH_LOCATION', 'FL_15FT_BASELINE_L', '15ft Baseline L',  1),
    ('FINISH_LOCATION', 'FL_15FT_BASELINE_R', '15ft Baseline R',  2),
    ('FINISH_LOCATION', 'FL_15FT_WING_L',     '15ft Wing L',      3),
    ('FINISH_LOCATION', 'FL_15FT_WING_R',     '15ft Wing R',      4),
    ('FINISH_LOCATION', 'FL_CORNER_3_L',      'Corner 3 L',       5),
    ('FINISH_LOCATION', 'FL_CORNER_3_R',      'Corner 3 R',       6),
    ('FINISH_LOCATION', 'FL_L_BLOCK',         'L Block',          7),
    ('FINISH_LOCATION', 'FL_L_ELBOW',         'L Elbow',          8),
    ('FINISH_LOCATION', 'FL_MIDDLE_OF_KEY',   'Middle of Key',    9),
    ('FINISH_LOCATION', 'FL_R_BLOCK',         'R Block',         10),
    ('FINISH_LOCATION', 'FL_R_ELBOW',         'R Elbow',         11),
    ('FINISH_LOCATION', 'FL_TOP_OF_FT',       'Top of FT',       12),
    ('FINISH_LOCATION', 'FL_TOP_OF_KEY_3PT',  'Top of Key 3pt',  13),
    ('FINISH_LOCATION', 'FL_3PT_WING_L',      '3pt Wing L',      14),
    ('FINISH_LOCATION', 'FL_3PT_WING_R',      '3pt Wing R',      15),

    -- FINISH_TYPE (3)
    ('FINISH_TYPE', 'BACKBOARD',       'Backboard',       1),
    ('FINISH_TYPE', 'CATCH_AND_SHOOT', 'Catch and Shoot', 2),
    ('FINISH_TYPE', 'REGULAR',         'Regular',         3),

    -- OTHER (1)
    ('OTHER', 'FREE_THROW_ROUTINE', 'Free Throw Routine', 1)

) AS v(cat_code, code, name, display_order) ON c.code = v.cat_code;
