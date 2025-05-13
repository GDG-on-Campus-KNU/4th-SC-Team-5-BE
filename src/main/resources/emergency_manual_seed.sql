INSERT INTO emergency_manual (id, emergency_type, title, description, steps, warning, updated_at) VALUES
-- 🔥 Burns (BURNS)
(1, 'BURNS', 'Burns from Hot Water', 'Basic first aid for burns caused by hot water.',
 '1. Cool the burned area under running cold water for 10-15 minutes.\n2. Cover with a clean gauze.\n3. Consider visiting a hospital if the pain is severe.',
 'Do not apply ice directly or rub the skin.', NOW()),

(2, 'BURNS', 'Chemical Burns', 'First aid for burns caused by chemical substances.',
 '1. Immediately rinse the affected area with running water for more than 20 minutes.\n2. Remove contaminated clothing.\n3. Seek medical attention immediately if pain persists or if the eyes are exposed.',
 'Do not rub your eyes during rinsing.', NOW()),

(3, 'BURNS', 'Electrical Burns', 'First aid for burns caused by electrical accidents.',
 '1. Disconnect the power source before approaching.\n2. Prepare for CPR in case of cardiac arrest.\n3. Do not cool the burn with water; prioritize transporting the patient to the hospital.',
 'Do not touch the patient while electricity is still active.', NOW()),

-- 🦴 Fracture (FRACTURE)
(4, 'FRACTURE', 'Arm Fracture', 'First aid for a broken arm.',
 '1. Immobilize the arm with a splint or firm object.\n2. Transport to the hospital without moving the arm.',
 'Do not attempt to realign the bone.', NOW()),

(5, 'FRACTURE', 'Leg Fracture', 'First aid for a broken leg.',
 '1. Stabilize both sides of the leg.\n2. Minimize movement as much as possible.\n3. Call an ambulance immediately.',
 'Do not shake the injured area.', NOW()),

-- 💉 Bleeding (BLEEDING)
(6, 'BLEEDING', 'Nosebleed', 'First aid for a nosebleed.',
 '1. Tilt the head slightly forward and gently pinch the nose.\n2. Visit a hospital if bleeding continues for more than 10 minutes.',
 'Do not tilt the head backward.', NOW()),

(7, 'BLEEDING', 'Severe Bleeding', 'First aid for severe bleeding.',
 '1. Firmly press the wound with a clean cloth.\n2. Keep the bleeding area elevated above the heart.\n3. Call an ambulance if bleeding does not stop.',
 'Do not rub or stimulate the bleeding area.', NOW()),

-- 🚑 CPR (CPR)
(8, 'CPR', 'Adult Cardiac Arrest', 'CPR procedure for adults.',
 '1. Check for responsiveness.\n2. Call 911 (or the local emergency number) and start chest compressions immediately.\n3. Use an AED if available.',
 'Follow emergency operator instructions if unfamiliar with AED use.', NOW()),

(9, 'CPR', 'Infant Cardiac Arrest', 'CPR procedure for infants.',
 '1. Compress the center of the chest with two fingers.\n2. Gently perform 30 compressions and 2 rescue breaths.',
 'Do not apply adult chest compression strength to an infant.', NOW()),

-- 🦱 Choking (CHOKING)
(10, 'CHOKING', 'Choking on Food', 'First aid for airway obstruction caused by food.',
 '1. Encourage the person to cough.\n2. Perform abdominal thrusts (Heimlich maneuver) if ineffective.\n3. Start CPR immediately if the person becomes unconscious.',
 'Do not slap the back hard or forcibly induce vomiting.', NOW()),

(11, 'CHOKING', 'Infant Choking', 'Response to airway obstruction in infants under 1 year.',
 '1. Place the infant face-down on your forearm and give 5 back blows.\n2. Perform 5 chest thrusts alternately.\n3. Call an ambulance if symptoms persist.',
 'Do not apply adult Heimlich maneuver to an infant.', NOW()),

-- 🫢 Hypothermia (HYPOTHERMIA)
(12, 'HYPOTHERMIA', 'Mild Hypothermia', 'First aid for mild hypothermia.',
 '1. Remove wet clothing and put on dry clothing.\n2. Wrap the patient in a warm blanket.\n3. Provide warm (non-alcoholic) drinks.',
 'Do not immerse the patient directly into hot water.', NOW()),

(13, 'HYPOTHERMIA', 'Severe Hypothermia', 'First aid for severe hypothermia.',
 '1. Keep the patient still and avoid unnecessary movement.\n2. Gradually warm the patient.\n3. Transport immediately to a hospital.',
 'Do not rub the body vigorously or rewarm too rapidly.', NOW()),

-- ☀️ Heatstroke (HEATSTROKE)
(14, 'HEATSTROKE', 'Heatstroke During Outdoor Activities', 'First aid for heatstroke during outdoor activities.',
 '1. Move immediately to a cool place.\n2. Cool the body and hydrate adequately.\n3. Call an ambulance if the patient is unconscious.',
 'Do not force water into an unconscious person.', NOW()),

(15, 'HEATSTROKE', 'Heatstroke During Exercise', 'First aid for heatstroke caused by exercise.',
 '1. Stop exercising and move to a shaded area.\n2. Cool the body using ice packs or a cold shower.\n3. Seek hospital care if symptoms persist.',
 'Avoid alcohol and caffeine.', NOW()),

-- ☠️ Poisoning (POISONING)
(16, 'POISONING', 'Drug Overdose', 'First aid for drug overdose situations.',
 '1. Identify the drug name and amount taken.\n2. Do not induce vomiting.\n3. Transport to the emergency room immediately.',
 'Do not administer antidotes or medicines without professional instruction.', NOW()),

(17, 'POISONING', 'Chemical Poisoning', 'First aid for chemical exposure poisoning.',
 '1. Rinse exposed areas immediately with water.\n2. Remove contaminated clothing.\n3. Move to fresh air if inhaled and seek hospital care.',
 'Do not stay in confined spaces.', NOW()),

-- ⚡ Seizure (SEIZURE)
(18, 'SEIZURE', 'Generalized Seizure', 'First aid for generalized seizures.',
 '1. Clear dangerous objects around the patient.\n2. Allow the seizure to occur naturally without restraining.\n3. Call emergency services if the seizure lasts more than 5 minutes.',
 'Do not put objects in the patient’s mouth.', NOW()),

(19, 'SEIZURE', 'Partial Seizure', 'First aid for partial seizures.',
 '1. Move the patient to a quiet place and let them rest.\n2. Monitor until full recovery of consciousness.',
 'Do not overwhelm the patient with questions after awakening.', NOW());
