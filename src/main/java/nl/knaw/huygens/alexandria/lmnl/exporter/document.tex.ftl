\documentclass{article}
\usepackage{incgraph,tikz,latexsym}
\usepackage[utf8x]{inputenc}
\usetikzlibrary{arrows,arrows.meta,decorations.pathmorphing,backgrounds,positioning,fit,graphs,shapes}
\newcommand{\n}{\textbackslash n}
\newcommand{\s}{\textvisiblespace}

\begin{document}
  \begin{inctext}
    \pagenumbering{gobble}% Remove page numbers (and reset to 1)
    \begin{tikzpicture}[
       textnode/.style={rectangle,draw=black!50,thick,rounded corners},
       textrange/.style={rectangle,draw=blue!50,thick},
       document/.style={circle,draw=black!50,thick}
     ]
    \node[document] (doc) {document};
    ${body}
    \end{tikzpicture}
  \end{inctext}
\end{document}
