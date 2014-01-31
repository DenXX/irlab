
import subprocess
from django.template.loader import render_to_string
from trecsearchui.settings import RETRIEVEAPPPATH, INDRIINDEXPATH, GETDOCAPPPATH

def read_topics(topics_file):
    topics = {}
    with open(topics_file, 'r') as input:
        curtopic = {"number": None, "title": "", "description": "", "narr": ""}
        readingfield = None
        for line in input:
            line = line.strip()
            if line.startswith("</top>"):
                topics[curtopic["number"]] = curtopic
                curtopic = {"number": None, "title": "", "description": "", "narr": ""}
                readingfield = None
            elif line.startswith("<num>"):
                curtopic["number"] = int(line.split(":")[-1].strip())
            elif line.startswith("<title>"):
                readingfield = 1
                curtopic["title"] = line.split(">")[-1]
            elif line.startswith("<desc>"):
                readingfield = 2
                curtopic["description"] = line.split(">")[-1]
            elif line.startswith("<narr>"):
                readingfield = 3
                curtopic["narr"] = line.split(">")[-1]
            elif line.startswith("<"):
                readingfield = None
            elif readingfield != None:
                if readingfield == 1:
                    curtopic["title"] += line + " "
                elif readingfield == 2:
                    curtopic["description"] += line + " "
                elif readingfield == 3:
                    curtopic["narr"] += line + " "

    return topics

def get_search_results(count, ispassages, query):
    params = {"index" : INDRIINDEXPATH }
    template = render_to_string("query_params_template.xml", params).replace("\n", "") + "\n"
    process = subprocess.Popen([RETRIEVEAPPPATH,], stdin = subprocess.PIPE, stdout = subprocess.PIPE)
    output, errors = process.communicate(template + "%d\t%d\t%s" % (count, ispassages, query))
    if process.returncode != 0 or errors != None:
        print process.returncode, errors
        return None
    return [x.split("\t") for x in output.strip().split("\n\n")]

def get_doc_text(doc_id):
    process = subprocess.Popen([GETDOCAPPPATH, INDRIINDEXPATH, doc_id], stdin = subprocess.PIPE, stdout = subprocess.PIPE)
    output, errors = process.communicate()
    if process.returncode != 0 or errors != None:
        print process.returncode, errors
        return None
    return output

def read_qrels(qrels_filepath, topic = None):
    res = {}
    with open(qrels_filepath, 'r') as input:
        for line in input:
            curtopic, tmp, docid, rel = line.strip().split()
            curtopic = int(curtopic)
            if topic and topic != curtopic:
                continue
            rel = int(rel)
            if curtopic not in res:
                res[curtopic] = {}
            res[curtopic][docid] = rel

    return res if not topic else res[topic]


def calculate_map(results, qrels):
    map_res = 0
    relcount = 0
    pos = 0
    for res in results:
        res = res[0]
        pos += 1
        if res in qrels and qrels[res] > 0:
            relcount += 1
            map_res += 1.0 * relcount / pos
    
    return map_res / relcount if relcount else 0.0
