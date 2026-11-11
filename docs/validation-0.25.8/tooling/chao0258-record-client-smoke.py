#!/usr/bin/env python3
"""Bind the focused native0258 review to the exact production JAR; images remain private."""
from pathlib import Path
import hashlib,json,re,sys,zipfile
from chao_artifact_io import write_json_atomic,copy_atomic
from chao_log_capture import coherent_console
root=Path(sys.argv[1]).resolve();original=Path(sys.argv[2]).resolve();run=Path(sys.argv[3]).resolve();plan=Path(sys.argv[4]).resolve();notes_file=Path(sys.argv[5]).resolve()
proof=root/'docs/validation-0.25.8';log=proof/'client-smoke.txt';copy_atomic(original,log)
def sha(p):
 with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
raw=log.read_text(errors='replace');content=coherent_console(log.read_bytes()).decode(errors='replace')
assert 'BUILD SUCCESSFUL' in content and 'BUILD FAILED' not in content
assert re.search(r'CHAO_WORLD0255_MODELS states=[1-9][0-9]* missing=0 actualNativeModelManager=true',content)
markers=[
 'CHAO_WORLD0258_SCOPE quickDoorRayFixture=true focus=reverse-doors,classroom-only,pupil-singing,play-city,raised-dark-trees,area-music',
 'CHAO_WORLD0255_QUICK_RAYS panels=16 airAndCurveHoles=13 actualClientLevelClip=true collision=true originalStatesRestored=true obsoleteSteppedMatrix=false',
 'CHAO_WORLD0258_PUPILS count=4 classroom=4 foyer=0 classroomSingers=2 babies=true actualSyncedBehavior=SING actualLesson=song dedicatedSingingEvents=2 actualSoundManagerActive=true source=NEUTRAL fixtureClock=true directBehaviorForced=false audibleListeningClaim=false',
 'CHAO_WORLD0258_FOYER pupils=0 classroom=4 actualClientEntities=true',
 'CHAO_WORLD0258_DARK_TREES authoredTrees=3 raisedPods=12 podY=82 branchY=83 nativeSupports=true oldLowPods=0',
 'CHAO_WORLD0257_MUSIC availableNeutral=true optionalOtherTracks=5 optionalOtherAssetsPresent=0 actualSoundManagerActive=true source=MUSIC nativeStream=true audioSink=null audibleListeningClaim=false',
 'CHAO_WORLD0257_MUSIC_EXIT currentTrack=NONE actualPriorStreamStopped=true missingRegionAsset=true',
 'CHAO_WORLD0253_ACTION_VERIFIED scene=school-entrance-reverse action=native-right-click-door actualOpen=true mode=ADVENTURE',
 'CHAO_WORLD0253_ACTION_ACK scene=school-entrance-reverse',
 'CHAO_WORLD0253_ACTION_PREDICTED scene=school-entrance-reverse mask=2097151 parts=21',
 'CHAO_WORLD0255_DARK_BAKE loaded=true biome=chao_garden:dark_twilight ordinaryClient=true developmentPresetAbsent=true',
 'CHAO_WORLD0255_INITIAL_READY realms=5']
for marker in markers:assert marker in content,marker
assert re.search(r'CHAO_WORLD0258_PLAY classroom=4 foyer=0 actualSyncedPlaying=[1-9][0-9]* nativeToyBlockPlaying=true directToySessionForced=false',content)
assert re.search(r'CHAO_WORLD0257_HUB_ANCHOR scene=hero-first-direct mainSceneSubmitted=true clientAnchorPresent=true .*rendererPresent=true offScreen=true withinViewDistance=true',content)
assert re.search(r'CHAO_WORLD0257_HUB_ANCHOR scene=hero-gate-music-stop mainSceneSubmitted=true clientAnchorPresent=true',content)
assert 'normal_client_fixture_absent": true' in raw
assert 'CHAO_WORLD0253_RAY_MATRIX rays=288' not in content
plan_data=json.loads(plan.read_text());scene_count=len(plan_data['scenes']);frame_count=sum(2 if s.get('motion') else 1 for s in plan_data['scenes'])
assert plan_data['release']=='0.25.8' and 7<=scene_count<=11
assert f'CHAO_WORLD0253_COMPLETE scenes={scene_count} frames={frame_count} saves={frame_count} returnRestored=true' in content
assert all('scene='+s['key'] in content for s in plan_data['scenes'])
rows=re.findall(r'CHAO_WORLD0253_SAVED file=([^ ]+) stage=(\d+) sha256=([a-f0-9]{64})',content);assert len(rows)==frame_count
frames=[]
for name,stage,expected in rows:
 p=run/'screenshots'/name;assert p.is_file() and sha(p)==expected
 frames.append(dict(filename=name,stage=int(stage),sha256=expected,private_internal_only=True))
jar=root/'build/libs/chao_garden-0.25.8.jar';manifest=proof/'current-production-payloads.json';payloads=json.loads(manifest.read_text())['payloads']
with zipfile.ZipFile(jar) as z:
 assert z.testzip() is None
 for name,expected in payloads.items():
  assert hashlib.sha256(z.read(name)).hexdigest()==expected
  p=(root/'build/classes/java/main'/name) if name.endswith('.class') else root/'src/main/resources'/name
  assert p.is_file() and sha(p)==expected,('Client production payload differs from candidate',name)
review=json.loads(notes_file.read_text());assert review['native_stills_inspected'] is True and len(review['observations'])>=5
source=[log,manifest,root/'src/main/java/com/chaogarden/client/World0253VisualSmoke.java',root/'src/main/java/com/chaogarden/client/LobbySceneRenderer025.java',Path(__file__).resolve(),Path(__file__).resolve().parent/'chao0258-prepare-visual.py',Path(__file__).resolve().parent/'run-chao-client.py',Path(__file__).resolve().parent/'run-chao-client-check.py',Path(__file__).resolve().parent/'chao0258-client-directory.init.gradle']
report=dict(release='0.25.8',result='PASS',sources=[dict(file=p.relative_to(root).as_posix(),sha256=sha(p)) for p in source],native_model_states=int(re.search(r'CHAO_WORLD0255_MODELS states=(\d+)',content)[1]),missing_native_models=0,current_door_fixture_panels=16,current_door_air_and_curve_holes=13,current_door_native_prediction_parts=21,current_door_native_ack=True,obsolete_stepped_matrix_run=False,normal_flat_preset_vanilla_dimensions=3,authored_realms_loaded_and_prepared=5,dark_biome='chao_garden:dark_twilight',scene_count=scene_count,frame_count=frame_count,initial_direct_hub_scene_submitted=True,return_hub_scene_submitted=True,actual_school_pupils=4,actual_classroom_pupils=4,actual_foyer_pupils=0,actual_classroom_singers=2,dedicated_singing_events_observed=2,actual_vocal_sound_manager_active=True,actual_toy_play_observed=True,raised_dark_fruit_pods=12,dark_fruit_pod_y=82,dark_fruit_branch_y=83,neutral_music_actual_sound_manager_active=True,missing_optional_music_assets=5,area_exit_stops_actual_stream=True,audible_listening_claim=False,private_frame_hashes=frames,private_plan_sha256=sha(plan),production_jar_sha256=sha(jar),production_payloads_byte_identical=True,scope='Fresh ordinary integrated Minecraft client with real model-manager bake, direct Adventure door click,21-part client prediction and native server ACK. Focused internal stills cover reversed door swing and closed-frame jamb, separated classroom city meshes, four classroom-only babies, actual autonomous toy play and live offered-song animation. Both localized dedicated vocal events must become active in the real SoundManager; the OpenAL null sink does not establish an audible listening claim. Native client block states verify all12 higher Dark fruit pods and their horizontal supports. Provided Neutral area stream and real Hub-exit stop remain verified; five regional recordings remain absent. Camera and monotonic offered-song clock are disposable fixtures; no actor pose, direct vocal playback or toy session is forced. Images/save/plan are omitted from delivered source.')
report.update(artifact_sha256=sha(jar),sample_review_notes=review['observations'],native_stills_inspected=True,private_review_notes_sha256=sha(notes_file),private_assets_delivered=False)
write_json_atomic(proof/'client-smoke.json',report)
print(json.dumps({k:v for k,v in report.items() if k not in ('sources','private_frame_hashes')},indent=2))
