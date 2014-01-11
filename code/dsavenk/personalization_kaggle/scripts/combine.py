
import csv
from sys import argv
import operator

def main(output_filename, by_rank, input_files):
    data = {}

    for input_file in input_files:
        with open(input_file, 'r') as input:
            reader = csv.reader(input)
            next(reader, None)
            index = 0
            last_session_id = -1
            for line in reader:
                session_id = int(line[0])
                if session_id != last_session_id:
                    index = 0
                last_session_id = session_id
                url_id = int(line[1])
                if not by_rank:
                    score = float(line[2])
                if session_id not in data:
                    data[session_id] = {}
                if url_id not in data[session_id]:
                    data[session_id][url_id] = 0
                if not by_rank:
                    data[session_id][url_id] += score
                else:
                    data[session_id][url_id] += -index
                index += 1

    with open(output_filename, 'w') as out:
        print >> out, "SessionID,URLID"
        for session in data.iterkeys():
            urls = data[session].items()
            urls.sort(key = operator.itemgetter(1), reverse = True)
            for url in urls:
                print >> out, str(session) + "," + str(url[0])

if __name__ == "__main__":
    output_filename = argv[1]
    by_rank = True if argv[2] == "rank" else False
    main(output_filename, by_rank, argv[3:])