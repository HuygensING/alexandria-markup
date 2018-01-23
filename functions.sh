#!/usr/bin/env bash
function add-element-handler {
  name=$1
  lname=$(echo "${name}" | sed 's/^./\L&/')
  echo "addElementHandler(new ${name}Handler(),\"${lname}\");"
  echo "public static class ${name}Handler extends DefaultElementHandler {}"
}