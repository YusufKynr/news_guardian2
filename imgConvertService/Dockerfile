FROM python:3.10

RUN apt-get update && apt-get install -y tesseract-ocr
RUN pip install pytesseract opencv-python pillow

WORKDIR /app
COPY . .

CMD ["python", "ocr_script.py"]
