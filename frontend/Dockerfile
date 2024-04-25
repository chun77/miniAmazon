FROM python:3
RUN mkdir /frontend
COPY . /frontend
WORKDIR /frontend/amazon

RUN pip install Django
RUN pip install psycopg
RUN pip install hashids
RUN pip install Pillow

RUN chmod +x runserver.sh