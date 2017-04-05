 LaTeX      = latex		
 BibTeX     = bibtex
 DVItoPS    = dvips             
 PStoPDF    = ps2pdf

thesis.pdf: thesis.ps
	${PStoPDF} thesis.ps thesis.pdf
	-rm *.aux *.blg  *.log *.lof *.toc *.out *~ *.lot *.loa  *.dvi *.bbl 
	
thesis.ps: *.tex  thesis.bib
	${LaTeX} thesis
	${BibTeX} thesis
	${LaTeX} thesis
	${LaTeX} thesis
	${DVItoPS} -o thesis.ps thesis.dvi


