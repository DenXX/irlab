
import csv
from sys import argv
import operator

if __name__ == "__main__":
    with open(argv[1], 'r') as input:
        csv = csv.reader(input)
        next(csv, None)
        with open(argv[2], 'r') as prediction_input:
            last_session_id = -1
            urls = []
            print "SessionID,URLID"
            for line in csv:
                pred = prediction_input.readline()
                prediction = float(pred)
                session_id = int(line[1])
                if last_session_id != session_id and len(urls) != 0:
                    urls = sorted(urls, key=operator.itemgetter(0), reverse=True)
                    for url in urls:
                        print str(last_session_id) + "," + str(url[1]) # + "," + str(url[0])
                    urls = []

                last_session_id = session_id
                url_id = int(line[2])
                urls.append((prediction, url_id))
            if len(urls) != 0:
                urls = sorted(urls, key=operator.itemgetter(0), reverse=True)
                for url in urls:
                    print str(last_session_id) + "," + str(url[1]) # + "," + str(url[0])
