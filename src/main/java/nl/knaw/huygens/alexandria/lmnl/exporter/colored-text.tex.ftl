\documentclass{article}
\usepackage{xcolor,latexsym}
\usepackage[utf8x]{inputenc}
\definecolorseries{rangedepth}{hsb}{last}{yellow!25}{blue}
\newcommand{\TextNode}[2]{\fcolorbox{black}{rangedepth!![#1]}{\strut #2}}
\newcommand{\n}{\textbackslash n}
\newcommand{\s}{\textvisiblespace}

\begin{document}
  \pagenumbering{gobble}% Remove page numbers (and reset to 1)
  \resetcolorseries[${maxdepth+1}]{rangedepth}
  \noindent Number of ranges: <#list 0..maxdepth as i>\TextNode{${i}}{ ${i} }</#list>\\

  \noindent
${body}
\end{document}
