debug_test:
	g++ -std=c++0x -g session.cpp string_utils.cpp prepare_test.cpp -o prepare_test
test:
	g++ -std=c++0x -O3 session.cpp string_utils.cpp prepare_test.cpp -o prepare_test
debug_process:
	g++ -std=c++0x -g session.cpp string_utils.cpp stats_collector.cpp feature_generator.cpp process_log.cpp -o process_log
process:
	g++ -std=c++0x -O3 session.cpp string_utils.cpp stats_collector.cpp feature_generator.cpp process_log.cpp -o process_log
graph:
	g++ -std=c++0x -g session.cpp string_utils.cpp click_graph.cpp click_graph_feats.cpp -o click_graph_feats_all
features:
	g++ -std=c++0x -O3 session.cpp string_utils.cpp feature_generator2.cpp -o features
