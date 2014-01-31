
from django.shortcuts import render_to_response
from utils import *
from trecsearchui.settings import QRELSFILE, TOPICSFILE

def main(request):
    context = {}
    topics = read_topics(TOPICSFILE)
    context["topics"] = [topics[key] for key in sorted(topics.keys())]
    if request.GET.has_key("q"):
        query = request.GET["q"]
        topic = int(request.GET["t"])
        qrels = read_qrels(QRELSFILE, topic)
        results = get_search_results(10, True, query)
        results1000 = get_search_results(1000, False, query)
        display_results = []
        for res in results:
            display_results.append((res[0], res[1], res[2], qrels[res[1]] if res[1] in qrels else "not judged"))
        context["query"] = query
        context["results"] = display_results
        context["topic"] = topic
        context["curtopicdetails"] = topics[topic]
        context["map"] = calculate_map(results1000, qrels)
        return render_to_response("index.html", context)
    context["topic"] = sorted(topics.keys())[0]
    return render_to_response("index.html", context)

def show_doc_view(request, topic, doc_id):
    context = {'doc_id': doc_id, }
    context['query'] = request.GET["q"]
    context['doc_text'] = get_doc_text(doc_id)
    context['curtopicdetails'] = read_topics(TOPICSFILE)[int(topic)]
    return render_to_response('show_doc.html', context)