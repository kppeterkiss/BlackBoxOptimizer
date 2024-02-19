#include <iostream>
#include <cmath>

#define PI 3.14159265

int main(int argc, char* argv[])
{
	int n = argc - 1;
	const float A = 10.0;
	
	float s = 0.0;
	for(int i = 0; i < n; ++i) {
		float x = atof(argv[i+1]);
		s += x*x-A * cos(2*PI*x);
	}
	s += A*n;
	std::cout << "rastrigin " << s << std::endl;
	
	return 0;
}