FROM python:3.10

RUN apt-get update && apt-get install -y tesseract-ocr

RUN pip install --no-cache-dir fastapi
RUN pip install --no-cache-dir pytesseract

WORKDIR /app

COPY requirements.txt requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

CMD ["python", "-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8090", "--reload"]
