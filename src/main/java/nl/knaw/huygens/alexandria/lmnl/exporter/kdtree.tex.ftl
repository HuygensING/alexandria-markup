\documentclass[landscape]{article}

\usepackage[utf8]{inputenc}
\usepackage[T1]{fontenc}
\usepackage[margin=1in]{geometry}
\usepackage{incgraph,caption,tikz-qtree}
\usetikzlibrary{shadows,trees}

\begin{document}
	\tikzset{
		font=\small,
		%edge from parent fork down,
		level distance=1.5cm,
		textNodeAxis/.style={
			top color=white,
		    bottom color=blue!25,
		    circle,
		    minimum height=8mm,
		    draw=blue!75,
		    very thick,
		    drop shadow,
		    align=center,
		    text depth = 0pt
		},
		textRangeAxis/.style={
			top color=white,
		    bottom color=green!25,
		    circle,
		    minimum height=8mm,
		    draw=green!75,
		    very thick,
		    drop shadow,
		    align=center,
		    text depth = 0pt
		},
		edge from parent/.style={
			draw=black!50,
		    thick
	    }
	}
	
	\centering
	\begin{figure}
		\begin{tikzpicture}[level/.style={sibling distance=60mm/#1}]
${body}
		\end{tikzpicture}
		\centering
		\caption*{(a,b) = (TextNodeIndex, TextRangeIndex)}
	\end{figure}
\end{document}