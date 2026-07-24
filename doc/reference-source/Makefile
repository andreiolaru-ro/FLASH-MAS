.PHONY: all document slides clean install

all: document slides clean install
	@echo "==> All steps complete."

# Build document.tex — run a second pass only if the first succeeds
document:
	@echo "==> [document] Pass 1: pdflatex document.tex"
	@pdflatex -interaction=nonstopmode -halt-on-error document.tex > document.pass1.log 2>&1 && \
		echo "==> [document] Pass 1 succeeded. Running pass 2..." && \
		pdflatex -interaction=nonstopmode -halt-on-error document.tex > document.pass2.log 2>&1 && \
		echo "==> [document] Pass 2 succeeded. Build complete." || \
		(echo "==> [document] BUILD FAILED. See document.pass1.log / document.pass2.log"; exit 1)

# Build slides.tex — run a second pass only if the first succeeds
slides:
	@echo "==> [slides] Pass 1: pdflatex slides.tex"
	@pdflatex -interaction=nonstopmode -halt-on-error slides.tex > slides.pass1.log 2>&1 && \
		echo "==> [slides] Pass 1 succeeded. Running pass 2..." && \
		pdflatex -interaction=nonstopmode -halt-on-error slides.tex > slides.pass2.log 2>&1 && \
		echo "==> [slides] Pass 2 succeeded. Build complete." || \
		(echo "==> [slides] BUILD FAILED. See slides.pass1.log / slides.pass2.log"; exit 1)

# Run the cleanup script
clean:
	@echo "==> [clean] Running a_clean-tex.sh"
	-@./a_clean-tex.sh
	@echo "==> [clean] a_clean-tex.sh finished"

# Rename and move the final PDFs to the parent directory
install:
	@echo "==> [install] Renaming document.pdf -> Quick-Reference.pdf and moving to parent dir"
	@mv document.pdf ../Quick-Reference.pdf
	@echo "==> [install] Renaming slides.pdf -> Quick-Slides.pdf and moving to parent dir"
	@mv slides.pdf ../Quick-Slides.pdf
	@echo "==> [install] Move complete"
