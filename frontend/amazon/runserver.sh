#!/bin/bash

python3 manage.py makemigrations || echo "makemigrations failed with exit code $?"

python3 manage.py migrate || echo "migrate failed with exit code $?"

# if [ -d "./product_image" ]; then
#     chmod -R 775 ./product_image
# else
#     echo "Directory not found: ./product_image"
# fi

python3 manage.py loaddata initial_data_amazon.json || echo "loaddata failed with exit code $?"

#echo "Starting server..."

python3 manage.py runserver 0.0.0.0:8000 || echo "Failed to start server $?"