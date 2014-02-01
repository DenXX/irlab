
from django.shortcuts import render_to_response
from utils import *
from trecsearchui.settings import QRELSFILE, TOPICSFILE
from models import Problem

def main(request):
    context = {}
    topics = read_topics(TOPICSFILE)
    context["topics"] = [topics[key] for key in sorted(topics.keys())]
    if request.POST.has_key("q"):
        query = request.POST["q"]
        topic = int(request.POST["t"])
        qrels = read_qrels(QRELSFILE, topic)
        if len(query) == 0:
            query = topics[topic]["title"]
        results = get_search_results(10, True, query)
        problems = request.POST.getlist('problem')
        for rank, problem in enumerate(problems):
            if len(problem.strip()) > 0:
                Problem.objects.create(topic=topic, query=query, docid=int(results[rank][0]),
                    docno=results[rank][1], problem=problem, flag=0)

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
    context['doc_text'] = highlight_text(get_doc_text(doc_id), context['query'])
    context['curtopicdetails'] = read_topics(TOPICSFILE)[int(topic)]
    return render_to_response('show_doc.html', context)
